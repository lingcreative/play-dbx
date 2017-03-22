package dbx.api

import javax.inject.Inject

import dbx.jdbc.DataSourceTransactionManager
import dbx.transaction.PlatformTransactionManager
import play.api.db.DBApi

/**
  * A simple TransactionManager lookuper that delegates all transaction management operations to the DataSourceTransactionManager
  * for each DataSource looked up with `play.api.db.DBApi.database(name)`.
  */
class SimpleDBApiTransactionManagerLookup @Inject()(dbApi: DBApi) extends TransactionManagerLookup {

  @volatile
  private var managers = Map.empty[String, PlatformTransactionManager]

  override def lookup(resource: String): PlatformTransactionManager = {
    var manager = managers.get(resource)
    if (manager.isEmpty) {
      synchronized {
        manager = Some(new DataSourceTransactionManager(dbApi.database(resource).dataSource))
        managers = managers + ((resource, manager.get))
      }
    }
    manager.get
  }

}
