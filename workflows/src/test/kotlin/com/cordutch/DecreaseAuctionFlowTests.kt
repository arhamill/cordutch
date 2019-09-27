package com.cordutch

import com.cordutch.flows.CreateAuctionFlow
import com.cordutch.flows.CreateAuctionResponderFlow
import com.cordutch.flows.DecreaseAuctionFlow
import com.cordutch.flows.IssueAssetFlow
import com.cordutch.states.AuctionResponse
import com.cordutch.states.AuctionState
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.GBP
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class DecreaseAuctionFlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(listOf("com.cordutch"),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        c = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(a, b, c)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach {
            it.registerInitiatedFlow(CreateAuctionResponderFlow::class.java)
        }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    private fun createAuction() : AuctionResponse {
        val assetFuture = a.startFlow(IssueAssetFlow("My asset"))
        mockNetwork.runNetwork()
        val assetId = assetFuture.getOrThrow().id
        val future = a.startFlow(CreateAuctionFlow(assetId, 100.GBP issuedBy a.info.chooseIdentity(), listOf(b, c).map { it.info.chooseIdentityAndCert().party }))
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    @Test
    fun flowReturnsCorrectlyFormedSignedTx() {
        val auctionTx = createAuction().stx.tx
        val auction = auctionTx.outputsOfType<AuctionState>().single()
        val newPrice = auction.price - 10.of(auction.price.token)
        val future = a.startFlow(DecreaseAuctionFlow(auction.linearId, newPrice))
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()

        assert(stx.tx.inputs.single() == auctionTx.outRef<AuctionState>(auction).ref) {"Auction tx should be an input"}
        val newAuction = stx.tx.outputStates.single() as AuctionState
        assert(newAuction == auction.withNewPrice(newPrice)) { "Flow should only change price" }
        stx.verifyRequiredSignatures()
    }

    @Test
    fun auctionMustExist() {
        val future = a.startFlow(DecreaseAuctionFlow(UniqueIdentifier(), 100.GBP issuedBy a.info.chooseIdentity()))
        mockNetwork.runNetwork()
        assertFailsWith<IllegalArgumentException>("Should fail if auction doesn't exist") { future.getOrThrow() }
    }

    @Test
    fun priceMustGoDown() {
        val auctionTx = createAuction()
        val auction = auctionTx.stx.tx.outputsOfType<AuctionState>().single()
        val newPrice = auction.price + 10.of(auction.price.token)
        val future = a.startFlow(DecreaseAuctionFlow(auction.linearId, newPrice))
        mockNetwork.runNetwork()
        assertFailsWith<TransactionVerificationException>("Must fail when price goes up") { future.getOrThrow() }
    }

    @Test
    fun initiatorMustBeOwner() {
        val auctionTx = createAuction()
        val auction = auctionTx.stx.tx.outputsOfType<AuctionState>().single()
        val newPrice = auction.price - 10.of(auction.price.token)
        val future = b.startFlow(DecreaseAuctionFlow(auction.linearId, newPrice))
        mockNetwork.runNetwork()
        assertFailsWith<NoSuchElementException>("Must fail when initiator is not owner") { future.getOrThrow() }
    }
}