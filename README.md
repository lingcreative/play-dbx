play-dbx [:point_right: 中文说明](README_zh.md)
=================================
A transaction management library that makes it simple while using transactional resources, e.g. play-jdbc, anorm, etc, in PlayFramework.
The original source code is migrated from **Spring Framekwork** 's `spring-tx`, `spring-jdbc` and `spring-context` modules, and remove any unnessery classes. It's aim is to provide a clean and no external dependencies transaction management framework for `Play Framework`. It supports `Play DBApi` a.k. `play-jdbc` out of box, you can refer the instructions bellow.
The core component/interface of `play-dbx` is `Transactional`, `TransactionSettings` and `TransactionManagerLookup`, and the default implementation of `Transactional` is `SimpleDBApiTransactional` and `SimpleDBApiTransactionManagerLookup` for `TransactionManagerLookup`, pelease refere the source code for more details. It only supports **Scala 2.11 && Play 2.5+** currently.

## Add dependency to you project（build.sbt）
```sbt
libraryDependencies += "com.lingcreative" %% "play-dbx" % "1.0.3"
```
## Steps(take `Anorm` for example)

#### Config bindings for Dependency Injection
```scala
// app/Module.scala

class Module extends AbstractModule {
  override def configure(): Unit = {
    //Use the out-of-boxed TransactionManagerLookup for DBApi, which create a
    //individual DataSourceTransactionManager
    //for each DataSource looked up from `DBApi.database(name).datasource`
    bind(classOf[TransactionManagerLookup]).to(classOf[SimpleDBApiTransactionManagerLookup])

    //Use the out-of-boxed Transactional for DBApi, which gets a connection from `DBApi.database(name).datasource`
    //and bind it to the current transaction context
    val transactionalKey = new TypeLiteral[Transactional[Connection]](){}
    bind(transactionalKey).to(classOf[SimpleDBApiTransactional])

    //Make the db named `default1` configured in application.conf as the default datasource,
    //and set JDBC isolation level to repeatable read
    //Refer [[dbx.transaction.Transactional]]、[[dbx.transaction.Transactional.TransactionSettings]] for details.
    val settings = TransactionSettings(isolation = Isolation.REPEATABLE_READ, resource = "default1")
    bind(classOf[TransactionSettings]).toInstance(settings)
  }
}

```

#### Make code transactional(modified from [play-anorm](https://github.com/playframework/play-scala-anorm-example))
```scala
import java.sql.Connection
import javax.inject._

import anorm.SqlParser._
import anorm._

// The main interface to use
import dbx.api.Transactional

case class Company(id: Option[Long] = None, name: String)

@Singleton
class CompanyService @Inject() (
        /*Inject the SimpleDBApiTransactional for JDBC operations*/
        transactional: Transactional[Connection]) {

  /**
   * Parse a Company from a ResultSet
   */
  val simple = {
    get[Option[Long]]("company.id") ~
      get[String]("company.name") map {
      case id~name => Company(id, name)
    }
  }

  /**
   * Get connection from `default1` db configured in application.conf,
   * and rollback if any `Exception` thrown
   */
  def options: Seq[(String,String)] = transactional() { implicit connection =>
    SQL("select * from company order by name").as(simple *).
      foldLeft[Seq[(String, String)]](Nil) { (cs, c) =>
      c.id.fold(cs) { id => cs :+ (id.toString -> c.name) }
    }
  }

  /**
   * Allow update and delete sql to execute, obtain connections from `default`,
   * and rollback if any `CompanyExistException` thrown, it equals to 
   * `@Transactional(readOnly=true, transactionManager="default", rollbackFor={CompanyExistException.class})`
   * in SpringFramework.
   */
  def save(company: Company): Unit = transactional(readOnly = false, resource = "default",
        rollbackFor = Array(classOf[CompanyExistException])) { implicit connection =>
    // execute sql update statements here ...
  }

}

```


**Enjoy it!** :tea:
