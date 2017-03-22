package dbx.api

import dbx.transaction.TransactionDefinition

/**
  * Created by sauntor on 17-3-21.
  */
object Propagation extends Enumeration {

  type Propagation = Value

  /**
    * Support a current transaction, create a new one if none exists.
    * Analogous to EJB transaction attribute of the same name.
    * <p>This is the default setting of a transaction annotation.
    */
  val REQUIRED = Value(TransactionDefinition.PROPAGATION_REQUIRED)

  /**
    * Support a current transaction, execute non-transactionally if none exists.
    * Analogous to EJB transaction attribute of the same name.
    * <p>Note: For transaction managers with transaction synchronization,
    * PROPAGATION_SUPPORTS is slightly different from no transaction at all,
    * as it defines a transaction scope that synchronization will apply for.
    * As a consequence, the same resources (JDBC Connection, Hibernate Session, etc)
    * will be shared for the entire specified scope. Note that this depends on
    * the actual synchronization configuration of the transaction manager.
    * @see dbx.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
    */
  val SUPPORTS = Value(TransactionDefinition.PROPAGATION_SUPPORTS)

  /**
    * Support a current transaction, throw an exception if none exists.
    * Analogous to EJB transaction attribute of the same name.
    */
  val MANDATORY = Value(TransactionDefinition.PROPAGATION_MANDATORY)

  /**
    * Create a new transaction, and suspend the current transaction if one exists.
    * Analogous to the EJB transaction attribute of the same name.
    * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
    * on all transaction managers. This in particular applies to
    * {@link dbx.transaction.jta.JtaTransactionManager},
    * which requires the {@code javax.transaction.TransactionManager} to be
    * made available it to it (which is server-specific in standard Java EE).
    * @see dbx.transaction.jta.JtaTransactionManager#setTransactionManager
    */
  val REQUIRES_NEW = Value(TransactionDefinition.PROPAGATION_REQUIRES_NEW)

  /**
    * Execute non-transactionally, suspend the current transaction if one exists.
    * Analogous to EJB transaction attribute of the same name.
    * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
    * on all transaction managers. This in particular applies to
    * {@link dbx.transaction.jta.JtaTransactionManager},
    * which requires the {@code javax.transaction.TransactionManager} to be
    * made available it to it (which is server-specific in standard Java EE).
    * @see dbx.transaction.jta.JtaTransactionManager#setTransactionManager
    */
  val NOT_SUPPORTED = Value(TransactionDefinition.PROPAGATION_NOT_SUPPORTED)

  /**
    * Execute non-transactionally, throw an exception if a transaction exists.
    * Analogous to EJB transaction attribute of the same name.
    */
  val NEVER = Value(TransactionDefinition.PROPAGATION_NEVER)

  /**
    * Execute within a nested transaction if a current transaction exists,
    * behave like PROPAGATION_REQUIRED else. There is no analogous feature in EJB.
    * <p>Note: Actual creation of a nested transaction will only work on specific
    * transaction managers. Out of the box, this only applies to the JDBC
    * DataSourceTransactionManager when working on a JDBC 3.0 driver.
    * Some JTA providers might support nested transactions as well.
    * @see dbx.jdbc.DataSourceTransactionManager
    */
  val NESTED = Value(TransactionDefinition.PROPAGATION_NESTED)
}
