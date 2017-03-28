package play.api.db

import java.util.Properties
import javax.inject.Singleton
import javax.sql.DataSource

import _root_.dbx.btm.api.BtmDataSource
import com.typesafe.config.Config
import play.api._
import play.api.inject.Module
import play.api.libs.JNDI

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success, Try}

/**
  * HikariCP runtime inject module.
  */
class BtmPoolModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[ConnectionPool].to[BtmConnectionPool]
    )
  }
}

/**
  * HikariCP components (for compile-time injection).
  */
trait BtmPoolComponents {
  def environment: Environment

  lazy val connectionPool: ConnectionPool = {
    val pool = new BtmConnectionPool()
    pool
  }
}

@Singleton
class BtmConnectionPool() extends ConnectionPool {

  import BtmConnectionPool._

//  @Inject()
//  private var environment: Environment = _

  /**
    * Create a data source with the given configuration.
    *
    * @param name the database name
    * @param configuration the data source configuration
    * @return a data source backed by a connection pool
    */
  override def create(name: String, dbConfig: DatabaseConfig, configuration: Config): DataSource = {
    val config = new PlayConfig(configuration)

    Try {
      Logger.info(s"Creating Pool for datasource '$name'")

      val datasource = doCreate(name, dbConfig, config)

      // Bind in JNDI
      dbConfig.jndiName.foreach { jndiName =>
        JNDI.initialContext.rebind(jndiName, datasource)
        logger.info(s"datasource [$name] bound to JNDI as $jndiName")
      }

      datasource
    } match {
      case Success(datasource) => datasource
      case Failure(ex) => throw config.reportError(name, ex.getMessage, Some(ex))
    }
  }

  /**
    * Close the given data source.
    *
    * @param dataSource the data source to close
    */
  override def close(dataSource: DataSource) = {
    Logger.info("Shutting down connection pool.")
    dataSource match {
      case ds: BtmDataSource => ds.close()
      case _ => sys.error("Unable to close data source: not a BtmDataSource")
    }
  }
}

/**
  * HikariCP config
  */
object BtmConnectionPool {
  private val logger = Logger(classOf[BtmConnectionPool])

  def doCreate(name: String, dbConfig: DatabaseConfig, configuration: PlayConfig) = {

    val dataSource = new BtmDataSource

    val config = configuration.get[PlayConfig]("btm")

    // Essentials configurations
    config.get[Option[String]]("className").foreach(dataSource.setClassName(_))

    val driverProps = new Properties()

    dbConfig.url.foreach(driverProps.setProperty("url", _))
    dbConfig.driver.foreach(driverProps.setProperty("driver", _))

    dbConfig.username.foreach(driverProps.setProperty("username", _))
    dbConfig.password.foreach(driverProps.setProperty("password", _))

    import scala.collection.JavaConverters._

    if (config.underlying.hasPath("driverProps")) {
      val dataSourceConfig = config.get[PlayConfig]("driverProps")
      dataSourceConfig.underlying.root().keySet().asScala.foreach { key =>
        driverProps.setProperty(key, dataSourceConfig.get[String](key))
      }
    }
    dataSource.setDriverProperties(driverProps)

    def toSeconds(duration: Duration) = {
      (if (duration.isFinite()) duration.toSeconds
      else 0l).toInt
    }

    // Frequently used
    dataSource.setUniqueName(config.get[Option[String]]("poolName").getOrElse(name))
    dataSource.setMaxPoolSize(config.get[Option[Int]]("maxPoolSize").get)
    dataSource.setMinPoolSize(config.get[Option[Int]]("minPoolSize").get)
    config.get[Option[Int]]("acquireIncrement").foreach { d => dataSource.setMinPoolSize(d) }
    config.get[Option[Duration]]("maxIdleTimeout").foreach { d => dataSource.setMaxIdleTime(toSeconds(d)) }
    config.get[Option[String]]("isolationLevel").foreach(dataSource.setIsolationLevel(_))
    config.get[Option[String]]("localAutoCommit").foreach(dataSource.setLocalAutoCommit(_))

    // Infrequently used
    config.get[Option[FiniteDuration]]("acquisitionTimeout").foreach { d => dataSource.setAcquisitionTimeout(d.toSeconds.toInt) }
    config.get[Option[Boolean]]("allowLocalTransactions").foreach(dataSource.setAllowLocalTransactions(_))
    config.get[Option[Boolean]]("enableJdbc4ConnectionTest").foreach(dataSource.setEnableJdbc4ConnectionTest(_))
    config.get[Option[String]]("testQuery").foreach(dataSource.setTestQuery(_))
    config.get[Option[Boolean]]("shareTransactionConnections").foreach(dataSource.setShareTransactionConnections(_))
    config.get[Option[Boolean]]("applyTransactionTimeout").foreach(dataSource.setApplyTransactionTimeout(_))

    dataSource
  }
}