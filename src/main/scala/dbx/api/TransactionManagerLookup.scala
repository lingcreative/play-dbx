package dbx.api

import dbx.transaction.PlatformTransactionManager
import dbx.transaction.interceptor.TransactionAttribute
import dbx.util.{ConcurrentReferenceHashMap, StringUtils}

/**
  * Determine the specific transaction manager to use for the given transaction.
  */
trait TransactionManagerLookup {

  /**
    * Key to use to store the default transaction manager.
    */
  private val DEFAULT_TRANSACTION_MANAGER_KEY = new AnyRef

  private val transactionManagerCache = new ConcurrentReferenceHashMap[AnyRef, PlatformTransactionManager](4)

  def apply(attribute: TransactionAttribute): PlatformTransactionManager = {
    val qualifier = attribute.getQualifier
    val key = if (StringUtils.hasText(qualifier)) qualifier else DEFAULT_TRANSACTION_MANAGER_KEY
    var transactionManager = transactionManagerCache.get(key)
    if (transactionManager == null) {
      transactionManager = lookup(qualifier)
      transactionManagerCache.putIfAbsent(key, transactionManager)
    }
    //    if (tm == null) throw new IllegalStateException(s"No TransactionManager found for $txAttr")
    transactionManager
  }

  protected def lookup(qualifier: String): PlatformTransactionManager
}
