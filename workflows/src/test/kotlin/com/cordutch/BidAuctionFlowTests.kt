package com.cordutch

import com.cordutch.flows.*
import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.Amount
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.packageName
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.finance.POUNDS
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueFlow
import net.corda.finance.schemas.CashSchemaV1
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.test.assertFailsWith

class BidAuctionFlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(listOf("com.cordutch", "net.corda.finance.contracts.asset", CashSchemaV1::class.packageName),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        c = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(a, b, c)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach {
            it.registerInitiatedFlow(CreateAuctionResponderFlow::class.java)
            it.registerInitiatedFlow(BidAuctionResponderFlow::class.java)
        }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    private fun StartedMockNode.createAuction() : SignedTransaction {
        val assetFuture = this.startFlow(IssueAssetFlow("My asset"))
        mockNetwork.runNetwork()
        val asset = assetFuture.getOrThrow().tx.outputStates.single() as AuctionableAsset

        val future = this.startFlow(CreateAuctionFlow(asset.linearId, 100.POUNDS, listOf(b, c).map { it.info.chooseIdentityAndCert().party }))
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    private fun StartedMockNode.issueCash(amount: Amount<Currency>): Cash.State {
        val flow = CashIssueFlow(amount, OpaqueBytes.of(1.toByte()), mockNetwork.defaultNotaryIdentity)
        val future = this.startFlow(flow)
        mockNetwork.runNetwork()
        val cashTx = future.getOrThrow()
        return cashTx.stx.tx.outputStates.single() as Cash.State
    }

    @Test
    fun flowReturnsCorrectlyFormedSignedTx() {
        val auctionTx = a.createAuction().tx
        val auction = auctionTx.outputsOfType<AuctionState>().single()
        val bidder = b.info.chooseIdentityAndCert().party
        b.issueCash(auction.price)
        val future = b.startFlow(BidAuctionFlow(auction.linearId))
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()

        assert(auctionTx.outRef<AuctionState>(auction).ref in stx.inputs) {"Auction tx should be an input"}
        stx.verifyRequiredSignatures()
    }

    @Test
    fun mustBeValidAuction() {
        val future = b.startFlow(BidAuctionFlow(UniqueIdentifier()))
        mockNetwork.runNetwork()
        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }
    }

    @Test
    fun mustIncludeSufficientCash() {
        val auctionTx = a.createAuction().tx
        val auction = auctionTx.outputsOfType<AuctionState>().single()
        b.issueCash(auction.price - 10.POUNDS)
        val future = b.startFlow(BidAuctionFlow(auction.linearId))
        mockNetwork.runNetwork()
        assertFailsWith<InsufficientBalanceException> { future.getOrThrow() }
    }

    @Test
    fun mustBeBidder() {
        val auctionTx = a.createAuction().tx
        val auction = auctionTx.outputsOfType<AuctionState>().single()
        a.issueCash(auction.price)
        val future = a.startFlow(BidAuctionFlow(auction.linearId))
        mockNetwork.runNetwork()
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }
}