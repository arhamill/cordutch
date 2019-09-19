package com.cordutch

import com.cordutch.flows.*
import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.finance.POUNDS
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class EndAuctionFlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(listOf("com.cordutch"), notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        c = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(a, b, c)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach {
            it.registerInitiatedFlow(CreateAuctionResponderFlow::class.java)
            it.registerInitiatedFlow(EndAuctionFlowResponder::class.java)
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

    @Test
    fun flowReturnsCorrectlyFormedSignedTx() {
        val auctionTx = a.createAuction().tx
        val auction = auctionTx.outputsOfType<AuctionState>().single()
        val future = a.startFlow(EndAuctionFlow(auction.linearId))
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()

        assert(auctionTx.outRef<AuctionState>(auction).ref in stx.inputs) {"Auction tx should be an input"}
        assert(stx.tx.outputsOfType<AuctionableAsset>().single() == auction.asset.unlock()) { "Unlocked asset should be output" }
        stx.verifyRequiredSignatures()
    }

    @Test
    fun mustBeValidAuction() {
        val future = a.startFlow(EndAuctionFlow(UniqueIdentifier()))
        mockNetwork.runNetwork()
        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }
    }

    @Test
    fun mustBeOwner() {
        val auctionTx = a.createAuction().tx
        val auction = auctionTx.outputsOfType<AuctionState>().single()
        val future = b.startFlow(EndAuctionFlow(auction.linearId))
        mockNetwork.runNetwork()
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }
}