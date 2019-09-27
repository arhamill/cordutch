package com.cordutch.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.utilities.unwrap

@InitiatingFlow
@StartableByRPC
class InformTransactionFlow(val id: SecureHash, val counterParty: Party, val strOrdinal: Int) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val tx = serviceHub.validatedTransactions.getTransaction(id) ?: throw IllegalArgumentException("Can't find transaction")
        val session = initiateFlow(counterParty)
        session.send(strOrdinal)
        subFlow(SendTransactionFlow(session, tx))
    }
}

@InitiatedBy(InformTransactionFlow::class)
class InformTransactionResponderFlow(val flowSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val ordinal = flowSession.receive<Int>().unwrap { it }
        subFlow(ReceiveTransactionFlow(otherSideSession = flowSession, statesToRecord = StatesToRecord.values()[ordinal]))
    }
}