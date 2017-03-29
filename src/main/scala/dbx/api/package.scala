package dbx

import dbx.api.Isolation.Isolation
import dbx.api.Propagation.Propagation
import play.api.Logger

/**
  * Created by sauntor on 17-3-22.
  */
package object api {

  val logger = Logger("dbx")

  object defaultSettings {
    val readOnly: Boolean = true
    val isolation: Isolation = Isolation.DEFAULT
    val propagation: Propagation = Propagation.REQUIRED
    val timeout: Int = 6000
    val noRollbackFor: Seq[Class[_]] = Seq()
    val rollbackFor: Seq[Class[_]] = Seq(classOf[Exception])
    val resource: String = "default"
  }

}
