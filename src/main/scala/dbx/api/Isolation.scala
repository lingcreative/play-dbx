package dbx.api

import dbx.transaction.TransactionDefinition

/**
  * Created by sauntor on 17-3-21.
  */
object Isolation extends Enumeration {

  type Isolation = Value

  /**
    * Support a current transaction, create a new one if none exists.
    * Analogous to EJB transaction attribute of the same name.
    * <p>This is the default setting of a transaction annotation.
    */
  val DEFAULT = Value(TransactionDefinition.ISOLATION_DEFAULT)

  /**
    * A constant indicating that dirty reads, non-repeatable reads and phantom reads
    * can occur. This level allows a row changed by one transaction to be read by
    * another transaction before any changes in that row have been committed
    * (a "dirty read"). If any of the changes are rolled back, the second
    * transaction will have retrieved an invalid row.
    * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
    */
  val READ_UNCOMMITTED = Value(TransactionDefinition.ISOLATION_READ_UNCOMMITTED)

  /**
    * A constant indicating that dirty reads are prevented; non-repeatable reads
    * and phantom reads can occur. This level only prohibits a transaction
    * from reading a row with uncommitted changes in it.
    * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
    */
  val READ_COMMITTED = Value(TransactionDefinition.ISOLATION_READ_COMMITTED)

  /**
    * A constant indicating that dirty reads and non-repeatable reads are
    * prevented; phantom reads can occur. This level prohibits a transaction
    * from reading a row with uncommitted changes in it, and it also prohibits
    * the situation where one transaction reads a row, a second transaction
    * alters the row, and the first transaction rereads the row, getting
    * different values the second time (a "non-repeatable read").
    * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
    */
  val REPEATABLE_READ = Value(TransactionDefinition.ISOLATION_REPEATABLE_READ)

  /**
    * A constant indicating that dirty reads, non-repeatable reads and phantom
    * reads are prevented. This level includes the prohibitions in
    * {@code ISOLATION_REPEATABLE_READ} and further prohibits the situation
    * where one transaction reads all rows that satisfy a {@code WHERE}
    * condition, a second transaction inserts a row that satisfies that
    * {@code WHERE} condition, and the first transaction rereads for the
    * same condition, retrieving the additional "phantom" row in the second read.
    * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
    */
  val SERIALIZABLE = Value(TransactionDefinition.ISOLATION_SERIALIZABLE)
}

