package com.cordutch.states

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction

@CordaSerializable
data class TransactionAndStateId(val stx: SignedTransaction, val id: UniqueIdentifier)