package de.t14d3.rapunzellib.bootstrap;

/**
 * The role of a participant in the RapunzelLib lifecycle.
 */
public enum BootstrapOwnerRole {
    /** The participant created and owns the context. */
    OWNER,
    /** The participant borrows an existing context. */
    BORROWER
}
