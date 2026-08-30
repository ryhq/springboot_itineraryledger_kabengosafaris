package com.itineraryledger.kabengosafaris.DataTransfer;

/**
 * What to do about a record the target already has.
 *
 * SKIP is the default because the usual import is into a company that is already trading, where
 * quietly overwriting a negotiated rate somebody has since corrected would be worse than doing
 * nothing. UPDATE has to be asked for, and it is the right answer when the bundle IS the correction.
 */
public enum TransferMode {
    SKIP,
    UPDATE
}
