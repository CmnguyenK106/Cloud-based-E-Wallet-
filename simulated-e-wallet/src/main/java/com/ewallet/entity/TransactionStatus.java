package com.ewallet.entity;

/**
 * Enum representing the lifecycle status of a transaction.
 *
 * PENDING — Transaction has been initiated but not yet finalised.
 *           Used as the initial state before processing completes.
 * SUCCESS — Transaction completed successfully; balances were mutated
 *           and the ledger entry is immutable.
 * FAILED  — Transaction failed validation or execution; balances were
 *           NOT mutated and the entry serves as an audit trail of the failure.
 */
public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED
}
