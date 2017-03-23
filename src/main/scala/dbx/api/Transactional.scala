package dbx.api

import javax.annotation.PostConstruct

import dbx.api.Isolation.Isolation
import dbx.api.Propagation.Propagation
import dbx.core.NamedThreadLocal
import dbx.transaction.interceptor._
import dbx.transaction.support.{CallbackPreferringPlatformTransactionManager, TransactionCallback}
import dbx.transaction.{NoTransactionException, PlatformTransactionManager, TransactionStatus, TransactionSystemException}

/**
  * Base class for transactional functions, such as the [[SimpleDBApiTransactional]].
  *
  * <p>This enables the underlying Spring transaction infrastructure to be used easily
  * to implement an aspect for any aspect system.
  *
  * <p>Subclasses are responsible for calling methods in this class in the correct order.
  *
  * <p>If no transaction name has been specified in the [[TransactionAttribute]],
  * the exposed name will be the {{{ fully-qualified class name + "." + method name }}}
  * (by default).
  *
  * <p>Uses the <b>Strategy</b> design pattern. A [[PlatformTransactionManager]]
  * implementation will perform the actual transaction management, and a
  * [[TransactionManagerLookup]] is used for determining the actual transaction manager.
  *
  * @author Rod Johnson
  * @author Juergen Hoeller
  * @author Stéphane Nicoll
  * @author Sam Brannen
  * @since 1.1
  * @see #setTransactionManager
  */
object Transactional {

  /**
    * Holder to support the `currentTransactionStatus()` method,
    * and to support communication between different cooperating advices
    * (e.g. before and after advice) if the aspect involves more than a
    * single method (as will be the case for around advice).
    */
  private val transactionInfoHolder = new NamedThreadLocal[TransactionInfo]("Current aspect-driven transaction")

  /**
    * Subclasses can use this to return the current TransactionInfo.
    * Only subclasses that cannot handle all operations in one method,
    * such as an AspectJ aspect involving distinct before and after advice,
    * need to use this mechanism to get at the current TransactionInfo.
    * An around advice such as an AOP Alliance MethodInterceptor can hold a
    * reference to the TransactionInfo throughout the aspect method.
    * <p>A TransactionInfo will be returned even if no transaction was created.
    * The {@code TransactionInfo.hasTransaction()} method can be used to query this.
    * <p>To find out about specific transaction characteristics, consider using
    * TransactionSynchronizationManager's {@code isSynchronizationActive()}
    * and/or {@code isActualTransactionActive()} methods.
    *
    * @return TransactionInfo bound to this thread, or { @code null} if none
    * @see TransactionInfo#hasTransaction()
    * @see TransactionSynchronizationManager#isSynchronizationActive()
    * @see TransactionSynchronizationManager#isActualTransactionActive()
    */
  @throws[NoTransactionException]
  protected def currentTransactionInfo: TransactionInfo = transactionInfoHolder.get

  /**
    * Return the transaction status of the current method invocation.
    * Mainly intended for code that wants to set the current transaction
    * rollback-only but not throw an application exception.
    *
    * @throws NoTransactionException if the transaction info cannot be found,
    *                                because the method was invoked outside an AOP invocation context
    */
  @throws[NoTransactionException]
  def currentTransactionStatus: TransactionStatus = {
    val info = currentTransactionInfo
    if (info == null || info.transactionStatus == null) throw new NoTransactionException("No transaction aspect-managed TransactionStatus in scope")
    info.transactionStatus
  }

  /**
    * Internal holder class for a Throwable, used as a return value
    * from a TransactionCallback (to be subsequently unwrapped again).
    */
  private class ThrowableHolder(val throwable: Throwable) {
    final def getThrowable: Throwable = this.throwable
  }

  /**
    * Internal holder class for a Throwable, used as a RuntimeException to be
    * thrown from a TransactionCallback (and subsequently unwrapped again).
    */
  private class ThrowableHolderException(val throwable: Throwable) extends RuntimeException(throwable) {
    override def toString: String = getCause.toString
  }

  /**
    * Opaque object used to hold Transaction information. Subclasses
    * must pass it back to methods on this class, but not see its internals.
    */
  sealed protected class TransactionInfo(val transactionManager: PlatformTransactionManager, val transactionAttribute: TransactionAttribute, val joinpointIdentification: String) {
    type Self = Transactional.TransactionInfo
    private var _transactionStatus: TransactionStatus = null
    private var _oldTransactionInfo: Self = null

    def newTransactionStatus(status: TransactionStatus) {
      _transactionStatus = status
    }

    def transactionStatus: TransactionStatus = _transactionStatus
    def oldTransactionInfo: Self = _oldTransactionInfo

    /**
      * Return whether a transaction was created by this aspect,
      * or whether we just have a placeholder to keep ThreadLocal stack integrity.
      */
    def hasTransaction: Boolean = _transactionStatus != null

    def bindToThread() {
      // Expose current TransactionStatus, preserving any existing TransactionStatus
      // for restoration after this transaction is complete.
      _oldTransactionInfo = transactionInfoHolder.get
      transactionInfoHolder.set(this)
    }

    def restoreThreadLocalStatus() {
      // Use stack to restore old transaction TransactionInfo.
      // Will be null if none was set.
      transactionInfoHolder.set(_oldTransactionInfo)
    }

    override def toString: String = transactionAttribute.toString
  }


  case class TransactionSettings(readOnly: Boolean = defaultSettings.readOnly, isolation: Isolation = defaultSettings.isolation,
                                 propagation: Propagation = defaultSettings.propagation, timeout: Int = defaultSettings.timeout,
                                 noRollbackFor: Array[Class[_]] = defaultSettings.noRollbackFor,
                                 rollbackFor: Array[Class[_]] = defaultSettings.rollbackFor, resource: String = defaultSettings.resource)
}

trait Transactional[R] {
  import Transactional._

  type Resource = R

  val lookupTransactionManager: TransactionManagerLookup
  val settings: TransactionSettings

  /**
    * Check that required properties were set.
    */
  @PostConstruct
  def initialize() {
    if (lookupTransactionManager == null) throw new IllegalStateException("Set the 'transactionManagerLookup' property or make sure to run within a BeanFactory " + "containing a PlatformTransactionManager bean!")
  }

  def apply[T](readOnly: Boolean = settings.readOnly, isolation: Isolation = settings.isolation,
               propagation: Propagation = settings.propagation, timeout: Int = settings.timeout,
               noRollbackFor: Array[Class[_]] = settings.noRollbackFor,
               rollbackFor: Array[Class[_]] = settings.rollbackFor, resource: String = settings.resource
              )(transactionalOperation: Resource => T): T = {

    val attribute = new RuleBasedTransactionAttribute
    attribute.setPropagationBehavior(propagation.id)
    attribute.setIsolationLevel(isolation.id)
    attribute.setTimeout(timeout)
    attribute.setReadOnly(readOnly)
    attribute.setQualifier(resource)
    val rollBackRules = new java.util.ArrayList[RollbackRuleAttribute]
    for (rbRule <- rollbackFor) {
      val rule = new RollbackRuleAttribute(rbRule)
      rollBackRules.add(rule)
    }
    for (rbRule <- noRollbackFor) {
      val rule = new NoRollbackRuleAttribute(rbRule)
      rollBackRules.add(rule)
    }
    attribute.getRollbackRules.addAll(rollBackRules)
    invokeWithinTransaction[T](transactionalOperation.getClass, attribute,
      () => transactionalOperation(obtainResource(resource)))
  }

  /**
    * Get a transactional resource for current transaction execution, create a new one if it not exists
    * @return
    */
  protected def obtainResource(resource: String): Resource

  protected def invokeWithinTransaction[T](transactionalFunction: Class[_], txAttr: TransactionAttribute, invocation: () => T): T = {
    val tm = lookupTransactionManager(txAttr)
    val joinpointIdentification: String = transactionalFunction.getName
    if (txAttr == null || !tm.isInstanceOf[CallbackPreferringPlatformTransactionManager]) {
      // Standard transaction demarcation with getTransaction and commit/rollback calls.
      val txInfo = createTransactionIfNecessary(tm, txAttr, joinpointIdentification)
      var retVal: Any = null
      try
        // This is an around advice: Invoke the next interceptor in the chain.
        // This will normally result in a target object being invoked.
        retVal = invocation()

      catch {
        case ex: Throwable => {
          // target invocation exception
          completeTransactionAfterThrowing(txInfo, ex)
          throw ex
        }
      } finally cleanupTransactionInfo(txInfo)
      commitTransactionAfterReturning(txInfo)
      return retVal.asInstanceOf[T]
    }
    else {
      // It's a CallbackPreferringPlatformTransactionManager: pass a TransactionCallback in.
      try {
        val result: Any = tm.asInstanceOf[CallbackPreferringPlatformTransactionManager].execute(txAttr, new TransactionCallback[Any]() {
          def doInTransaction(status: TransactionStatus): Any = {
            val txInfo = prepareTransactionInfo(tm, txAttr, joinpointIdentification, status)
            try {
              invocation()
            } catch {
              case ex: Throwable => {
                if (txAttr.rollbackOn(ex)) {
                  // A RuntimeException: will lead to a rollback.
                  if (ex.isInstanceOf[RuntimeException]) throw ex.asInstanceOf[RuntimeException]
                  else throw new ThrowableHolderException(ex)
                }
                else {
                  // A normal return value: will lead to a commit.
                  new ThrowableHolder(ex)
                }
              }
            } finally cleanupTransactionInfo(txInfo)
          }
        })
        // Check result: It might indicate a Throwable to rethrow.
        if (result.isInstanceOf[ThrowableHolder]) throw result.asInstanceOf[ThrowableHolder].getThrowable
        else return result.asInstanceOf[T]
      } catch {
        case ex: ThrowableHolderException => {
          throw ex.getCause
        }
      }
    }
  }



  /**
    * Create a transaction if necessary based on the given TransactionAttribute.
    * <p>Allows callers to perform custom TransactionAttribute lookups through
    * the TransactionAttributeSource.
    *
    * @param txAttr the TransactionAttribute (may be { @code null})
    * @param joinpointIdentification the fully qualified method name
    *                                (used for monitoring and logging purposes)
    * @return a TransactionInfo object, whether or not a transaction was created.
    *         The { @code hasTransaction()} method on TransactionInfo can be used to
    *                     tell if there was a transaction created.
    */
  protected def createTransactionIfNecessary(tm: PlatformTransactionManager, txAttr: TransactionAttribute, joinpointIdentification: String): TransactionInfo = {
    // If no name specified, apply method identification as transaction name.
    var attr:TransactionAttribute = txAttr
    if (txAttr != null && txAttr.getName == null) attr = new DelegatingTransactionAttribute(txAttr) {
      override def getName: String = joinpointIdentification
    }
    var status: TransactionStatus = null
    if (txAttr != null) if (tm != null) status = tm.getTransaction(attr)
    else if (logger.isDebugEnabled) logger.debug("Skipping transactional joinpoint [" + joinpointIdentification + "] because no transaction manager has been configured")
    prepareTransactionInfo(tm, txAttr, joinpointIdentification, status)
  }

  /**
    * Prepare a TransactionInfo for the given attribute and status object.
    *
    * @param txAttr the TransactionAttribute (may be { @code null})
    * @param joinpointIdentification the fully qualified method name
    *                                (used for monitoring and logging purposes)
    * @param status                  the TransactionStatus for the current transaction
    * @return the prepared TransactionInfo object
    */
  protected def prepareTransactionInfo(tm: PlatformTransactionManager, txAttr: TransactionAttribute, joinpointIdentification: String, status: TransactionStatus): TransactionInfo = {
    val txInfo = new TransactionInfo(tm, txAttr, joinpointIdentification)
    if (txAttr != null) {
      // We need a transaction for this method...
      if (logger.isTraceEnabled) logger.trace("Getting transaction for [" + txInfo.joinpointIdentification + "]")
      // The transaction manager will flag an error if an incompatible tx already exists.
      txInfo.newTransactionStatus(status)
    }
    else {
      // The TransactionInfo.hasTransaction() method will return false. We created it only
      // to preserve the integrity of the ThreadLocal stack maintained in this class.
      if (logger.isTraceEnabled) logger.trace("Don't need to create transaction for [" + joinpointIdentification + "]: This method isn't transactional.")
    }
    // We always bind the TransactionInfo to the thread, even if we didn't create
    // a new transaction here. This guarantees that the TransactionInfo stack
    // will be managed correctly even if no transaction was created by this aspect.
    txInfo.bindToThread()
    txInfo
  }

  /**
    * Execute after successful completion of call, but not after an exception was handled.
    * Do nothing if we didn't create a transaction.
    *
    * @param txInfo information about the current transaction
    */
  protected def commitTransactionAfterReturning(txInfo: TransactionInfo) {
    if (txInfo != null && txInfo.hasTransaction) {
      if (logger.isTraceEnabled) logger.trace("Completing transaction for [" + txInfo.joinpointIdentification + "]")
      txInfo.transactionManager.commit(txInfo.transactionStatus)
    }
  }

  /**
    * Handle a throwable, completing the transaction.
    * We may commit or roll back, depending on the configuration.
    *
    * @param txInfo information about the current transaction
    * @param ex     throwable encountered
    */
  protected def completeTransactionAfterThrowing(txInfo: TransactionInfo, ex: Throwable) {
    if (txInfo != null && txInfo.hasTransaction) {
      if (logger.isTraceEnabled) logger.trace("Completing transaction for [" + txInfo.joinpointIdentification + "] after exception: " + ex)
      if (txInfo.transactionAttribute.rollbackOn(ex)) try
        txInfo.transactionManager.rollback(txInfo.transactionStatus)
      catch {
        case ex2: TransactionSystemException => {
          logger.error("Application exception overridden by rollback exception", ex)
          ex2.initApplicationException(ex)
          throw ex2
        }
        case ex2: RuntimeException => {
          logger.error("Application exception overridden by rollback exception", ex)
          throw ex2
        }
        case err: Error => {
          logger.error("Application exception overridden by rollback error", ex)
          throw err
        }
      }
      else {
        // We don't roll back on this exception.
        // Will still roll back if TransactionStatus.isRollbackOnly() is true.
        try
          txInfo.transactionManager.commit(txInfo.transactionStatus)

        catch {
          case ex2: TransactionSystemException => {
            logger.error("Application exception overridden by commit exception", ex)
            ex2.initApplicationException(ex)
            throw ex2
          }
          case ex2: RuntimeException => {
            logger.error("Application exception overridden by commit exception", ex)
            throw ex2
          }
          case err: Error => {
            logger.error("Application exception overridden by commit error", ex)
            throw err
          }
        }
      }
    }
  }

  /**
    * Reset the TransactionInfo ThreadLocal.
    * <p>Call this in all cases: exception or normal return!
    *
    * @param txInfo information about the current transaction (may be { @code null})
    */
  protected def cleanupTransactionInfo(txInfo: TransactionInfo) {
    if (txInfo != null) txInfo.restoreThreadLocalStatus()
//    resourceNameHolder.remove()
//    resourceOperationHolder.remove()
  }


}
