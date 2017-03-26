package dbx.api

import java.sql.Connection
import javax.inject.{Inject, Singleton}

import com.google.inject.{AbstractModule, TypeLiteral}
import dbx.api.Transactional.TransactionSettings
import dbx.jdbc.{DataSourceTransactionManager, DataSourceUtils}
import dbx.transaction.PlatformTransactionManager
import play.api.db.DBApi

/**
  * A standard `Play` module to provide basic setup for DataSourceTransactionManager
  */
class SimpleModule extends AbstractModule {
  override def configure = {
    val transactionalKey = new TypeLiteral[Transactional[Connection]](){}
    bind(transactionalKey).to(classOf[SimpleDBApiTransactional])
    bind(classOf[TransactionManagerLookup]).to(classOf[SimpleDBApiTransactionManagerLookup])
  }
}

trait SimpleComponents {
  def dbApi: DBApi
  def transactionSettings: TransactionSettings
  def transactionManager: TransactionManagerLookup

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
