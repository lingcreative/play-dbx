play-dbx中文说明
================
play-dbx是一个事务管理框架/类库，源代码移植于SpringFramework，并去除无关类和依赖。核心接口为`Transactional`和`TransactionManagerLookup`(也是需要用户扩展定制的Trait)，并提供了DBApi的简单实现。

# 项目设置（build.sbt）
```sbt
libraryDependencies += "com.lingcreative" %% "play-dbx" % "1.0.0"
```
# 使用（Anorm）

## 依赖注入
```scala
//app/Module.scala

class Module extends AbstractModule {
  override def configure(): Unit = {
    //绑定默认DBApi的TransactionManagerLookup实现
    bind(classOf[TransactionManagerLookup]).to(classOf[SimpleDBApiTransactionManagerLookup])

    //绑定默认DBApi的Transactional实现
    val transactionalKey = new TypeLiteral[Transactional[Connection]](){}
    bind(transactionalKey).to(classOf[SimpleDBApiTransactional])

    //设定事物的默认属性（这里使事物级别为 __可重复读__ 并 使application.conf里配置的default1数据库源为默认数据源）
    //参考[[dbx.transaction.Transactional]]、[[dbx.transaction.Transactional.TransactionSettings]]
    val settings = TransactionSettings(isolation = Isolation.REPEATABLE_READ, resource = "default1")
    bind(classOf[TransactionSettings]).toInstance(settings)
  }
}

```

## 代码使用
```scala
import java.sql.Connection
import javax.inject._

import anorm.SqlParser._
import anorm._

// 导入主接口
import dbx.api.Transactional

case class Company(id: Option[Long] = None, name: String)

@Singleton
class CompanyService @Inject() (transactional: Transactional[Connection]/*注入Module.scala中配置的Transactional*/) {

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
   * 按照Module.scala中配置的属性执行事务操作, transaction对象的使用方法与Spring的@Transactional注解一致
   */
  def options: Seq[(String,String)] = transactional() { implicit connection =>
    SQL("select * from company order by name").as(simple *).
      foldLeft[Seq[(String, String)]](Nil) { (cs, c) =>
      c.id.fold(cs) { id => cs :+ (id.toString -> c.name) }
    }
  }

}

```
