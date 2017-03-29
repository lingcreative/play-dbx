集成BitronixTransactionManager的配置
==========================
```
// in application.conf
play {
    modules {
        # 启用BTM支持
        enabled += dbx.api.btm.BitronixDBApiModule
    }
    db {
        default {
            pool = "hikaricp"
            # HikariCP 配置
            hikaricp {
        
              # 让hikaricp使用BtmDataSource作为DataSource的实现, 因为PoolingDataSource的driverProperties无法在hikaricp里正确配置
              dataSourceClassName = "dbx.api.btm.BtmDataSource"
        
              # 数据源属性，即BTM中的PoolingDataSource的属性配置
              dataSource {
                # 这里以MariaDB为例，让PoolingDataSource知道底层XADataSource的实现类为MariaDbDataSource
                className = org.mariadb.jdbc.MariaDbDataSource
                uniqueName = default
                allowLocalTransactions = true
                minPoolSize = 5 # ``Required``
                maxPoolSize = 20 # ``Required``
                # 此属性为BtmDataSource中为了解决无法直接配置driverProperties属性而添加的，此路径中的资源文件会自动加载并把结果设置为driverProperties
                # "classpath://"前缀表示从类路径中加载, 也可以换成其他的URL，若无协议前缀（例如http://）则从本地文件系统加载
                driverConfig = "classpath://db.default.properties"
              }
        
              # 其他hikaricp配置选项...
              autoCommit = true
              connectionTimeout = 30 seconds
              idleTimeout = 10 minutes
              maxLifetime = 30 minutes
            }
        }
    }
}
dbx {
    #事务默认属性配置，参考[[TransactionSettings]]
    transactionSettings {
        #resource = default
        isolationLevel = REPEATABLE_READ
    }
}

```