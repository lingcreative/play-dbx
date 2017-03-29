BitronixTransactionManager Configuration
==========================
```
// in application.conf
play {
    modules {
        # Enable the BitronixDBApiModule for BTM [[Transactional[Connection]]] support
        enabled += dbx.api.btm.BitronixDBApiModule
    }
    db {
        default {
            pool = "hikaricp"
            # HikariCP configuration options
            hikaricp {
        
              # The datasource class name must be set to BtmDataSource, as for driverProperties configuring problem caused by hikaricp
              dataSourceClassName = "dbx.api.btm.BtmDataSource"
        
              # Data source configuration options, i.e, the properties need to configure on PoolingDataSource _in BTM_
              dataSource {
                className = org.mariadb.jdbc.MariaDbDataSource
                uniqueName = default
                allowLocalTransactions = true
                minPoolSize = 5 # ``Required``
                maxPoolSize = 20 # ``Required``
                # A workaround for PoolingDataSource's driverProperties property,
                # the "classpath://" prefix indicates that we'll try to load it from class path, and you can set it to any url,
                # if the path contains no schema info(e.g. http://), BtmDataSource will try to load it from local file system
                driverConfig = "classpath://db.default.properties"
              }
        
              # Other configuration goes here...
            }
        }
    }
}
dbx {
    # default settings for transaction management, refer [[TransactionSettings]] for details
    transactionSettings {
        resource = default
        isolationLevel = REPEATABLE_READ
    }
}

```