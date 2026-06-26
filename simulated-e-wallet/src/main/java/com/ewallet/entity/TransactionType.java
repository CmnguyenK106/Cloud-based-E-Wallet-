package com.ewallet.entity;

/**
 * Enum representing the type/category of a financial transaction.
 * Used to classify every ledger entry for auditability and reporting.
 *
 * TRANSFER — Outgoing P2P transfer from sender's wallet to receiver's wallet.
 * RECEIVE  — Incoming P2P transfer credited to receiver's wallet from a sender.
 * TOPUP    — Depositing funds into the user's own wallet.
 * WITHDRAW — Withdrawing funds out of the user's wallet.
 */
public enum TransactionType {
    TRANSFER,
    RECEIVE,
    TOPUP,
    WITHDRAW
}
