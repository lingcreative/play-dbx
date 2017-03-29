package dbx.api

import java.sql.Connection
import javax.inject.{Inject, Singleton}

import com.google.inject.{AbstractModule, Provider, TypeLiteral}
import dbx.api.Transactional.{TransactionSettings, TransactionSettingsBuilder}
import dbx.jdbc.{DataSourceTransactionManager, DataSourceUtils}
import dbx.transaction.PlatformTransactionManager
import play.api.Configuration
import play.api.Environment
import play.api.db.DBApi

/**
  * A standard `Play` module to provide basic setup for DataSourceTransactionManager
  */
class SimpleModule extends AbstractModule {
  override def configure = {
    val transactionalKey = new TypeLiteral[Transactional[Connection]](){}
    bind(transactionalKey).to(classOf[SimpleDBApiTransactional])
    bind(classOf[TransactionManagerLookup]).to(classOf[SimpleDBApiTransactionManagerLookup])
    val confProvider = binder().getProvider(classOf[Configuration])
    val envProvider = binder().getProvider(classOf[Environment])
    bind(classOf[TransactionSettings]).toProvider(new TransactionSettingsProvider(){
      override def config = confProvider.get()
      override def env = envProvider.get()
    })
  }
}

trait TransactionSettingsProvider extends Provider[TransactionSettings] {
  def config: Configuration
  def env: Environment
  override def get(): TransactionSettings = {
    config.getConfig("dbx.transactionSettings") match {
      case Some(settings) =>
        val builder = TransactionSettingsBuilder()
        settings.getString("resource").foreach(builder.resource = _)
        settings.getBoolean("readOnly").foreach(builder.readOnly = _)
        settings.getString("isolation").foreach{ s => builder.isolation = Isolation.withName(s) }
        settings.getString("propagation").foreach{ s => builder.propagation = Propagation.withName(s) }
        settings.getInt("timeout").foreach(builder.timeout = _)
        val cl = env.classLoader
        settings.getStringSeq("rollbackFor").foreach { seq => builder.rollbackFor = seq.map(cl.loadClass(_)) }
        settings.getStringSeq("noRollbackFor").foreach { seq => builder.noRollbackFor = seq.map(cl.loadClass(_)) }
        builder.build()
      case None => throw config.reportError("dbx.transactionSettings", "Can't load transactionSettings")
    }
  }
}

trait SimpleComponents {
  def environment: Environment
  def configuration: Configuration
  def dbApi: DBApi

  lazy val transactionManager: TransactionManagerLookup = new SimpleDBApiTransactionManagerLookup(dbApi)
  lazy val transactionSettings: TransactionSettings = new TransactionSettingsProvider(){
      override def config = configuration
      override def env = environment
    }.get()
  lazy val transactional: Transactional[Connection] = new SimpleDBApiTransactional(dbApi, transactionManager, transactionSettings)
}


/**
  * A simple Transactional function that uses jdbc Connection(obtained from DBApi and binding it to transaction
  * execution context) as a transactional resource.
  */
@Singleton
class SimpleDBApiTransactional @Inject()(dbApi: DBApi, override val lookupTransactionManager: TransactionManagerLookup,
                                         override val settings: TransactionSettings) extends Transactional[Connection] {

  override def obtainResource(resource: String): Resource = {
    DataSourceUtils.getConnection(dbApi.database(resource).dataSource)
  }

  override protected def releaseResource(resource: String, actualResource: Resource): Unit = {
    DataSourceUtils.releaseConnection(actualResource, dbApi.database(resource).dataSource)
  }
}

/**
  * A simple TransactionManager lookuper that delegates all transaction management operations to the DataSourceTransactionManager
  * for each DataSource looked up with `play.api.db.DBApi.database(name)`.
  */
@Singleton
class SimpleDBApiTransactionManagerLookup @Inject()(dbApi: DBApi) extends TransactionManagerLookup {

  @volatile
  private var managers = Map.empty[String, PlatformTransactionManager]

  override def lookup(resource: String): PlatformTransactionManager = {
    var manager = managers.get(resource)
    if (manager.isEmpty) {
      synchronized {
        manager = Some(new DataSourceTransactionManager(dbApi.database(resource).dataSource))
        managers = managers + (resource -> manager.get)
      }
    }
    manager.get
  }

}
