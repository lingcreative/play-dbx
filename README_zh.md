play-dbx中文说明
================
play-dbx是一个事务管理框架/类库，源代码移植于SpringFramework，并去除无关类和依赖。核心接口为`Transactional`和`TransactionManagerLookup`(也是需要用户扩展定制的Trait)，并提供了DBApi的简单实现。

#项目设置（build.sbt）
```sbt
libraryDependencies += "com.lingcreative" %% "play-dbx" % "1.0.0"
```
#使用（Anorm）
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
