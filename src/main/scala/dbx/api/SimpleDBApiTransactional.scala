package dbx.api

import java.sql.Connection
import javax.inject.Inject

import dbx.api.Transactional.TransactionSettings
import dbx.jdbc.DataSourceUtils
import play.api.db.DBApi

/**
  * A simple Transactional function that uses jdbc Connection(obtained from DBApi and binding it to transaction
  * execution context) as a transactional resource.
  */
class SimpleDBApiTransactional @Inject()(dbApi: DBApi, override val lookupTransactionManager: TransactionManagerLookup,
                                         override val settings: TransactionSettings) extends Transactional[Connection] {

  override def obtainConnection(resource: String): Resource = {
    DataSourceUtils.getConnection(dbApi.database(resource).dataSource)
  }

}