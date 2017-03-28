package dbx.btm.api

import java.io.{FileInputStream, IOException, InputStream}
import java.net.URL
import java.sql.Connection
import java.util.Properties
import java.util.logging.Logger
import javax.inject.{Inject, Singleton}

import bitronix.tm.resource.jdbc.PoolingDataSource
import bitronix.tm.{BitronixTransactionManager, TransactionManagerServices}
import com.google.inject.{AbstractModule, Provider, TypeLiteral}
import dbx.api.Transactional.TransactionSettings
import dbx.api.{SimpleDBApiTransactional, TransactionManagerLookup, Transactional}
import dbx.transaction.PlatformTransactionManager
import dbx.transaction.jta.JtaTransactionManager
import play.api.db.DBApi

/**
  * A standard `Play` module to provide basic setup for BitronixTransactionManager
  */
class BitronixModule extends AbstractModule {

  override def configure() = {
    bind(classOf[BitronixTransactionManager]).toProvider(classOf[BitronixTransactionManagerProvider])
    val transactionalKey = new TypeLiteral[Transactional[Connection]](){}
    bind(transactionalKey).to(classOf[SimpleDBApiTransactional])
    bind(classOf[TransactionManagerLookup]).to(classOf[BtmTransactionManagerLookup])
  }
}

trait BitronixComponents {
  def btm: BitronixTransactionManager
  def dbApi: DBApi
  def settings: TransactionSettings

  lazy val transactionManager: BtmTransactionManagerLookup = new BtmTransactionManagerLookup(btm)
  lazy val transactional: Transactional[Connection] = new SimpleDBApiTransactional(dbApi, transactionManager, settings)
}

@Singleton
class BitronixTransactionManagerProvider extends Provider[BitronixTransactionManager] {
  private lazy val btm = TransactionManagerServices.getTransactionManager
  override def get(): BitronixTransactionManager = btm
}

@Singleton
class BtmTransactionManagerLookup @Inject()(private val btm: BitronixTransactionManager) extends TransactionManagerLookup{

  private lazy val manager = {
    val jtaTm = new JtaTransactionManager(btm, btm)
    jtaTm.setAllowCustomIsolationLevels(true)
    jtaTm
  }

  override def lookup(resource: String): PlatformTransactionManager = {
    manager
  }
}

object BtmDataSource {
  private val CLASSPATH_PREFIX = "classpath://"
}

class BtmDataSource extends PoolingDataSource {
  import BtmDataSource._

  override def getParentLogger: Logger = Logger.getGlobal

  def setDriverConfig(path: String): Unit = {
    if (path != null) setDriverProperties(loadDriverConfig(path))
  }

  protected def loadDriverConfig(path: String) = {
    var res: InputStream = null
    try {
      if (path.startsWith(CLASSPATH_PREFIX)) {
        var cl = Thread.currentThread().getContextClassLoader
        cl = if (cl == null) classOf[BtmDataSource].getClassLoader else cl
        res = cl.getResourceAsStream(path.substring(CLASSPATH_PREFIX.length))
      } else if (sys.props("os.name") == "Windows" && path.indexOf(':') == 1 || path.indexOf(':') == -1) {
          res = new FileInputStream(path)
      } else {
        res = new URL(path).openStream()
      }
      if (res == null) {
        throw new IllegalArgumentException(s"driver config file: $path can't be found in classpath")
      }
      new Properties() { load(res) }
    } catch {
      case e: IOException => throw new IllegalStateException(s"Can't load config file from: $path", e)
    } finally {
      if (res != null) res.close()
    }
  }
}