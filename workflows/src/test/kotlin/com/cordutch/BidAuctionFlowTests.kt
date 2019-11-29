package com.cordutch

import com.cordutch.flows.*
import com.cordutch.states.AuctionResponse
import com.cordutch.states.AuctionState
import com.r3.corda.lib.tokens.contracts.schemas.TokenSchemaV1
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import com.r3.corda.lib.tokens.money.GBP
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.internal.packageName
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

class BidAuctionFlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode
    lateinit var issuer: Party

    @Before
    fun setup() {
        mockNetwork = MockNetwork(listOf("com.cordutch", "com.r3.corda.lib.tokens", TokenSchemaV1::class.packageName),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        c = mockNetwork.createNode(MockNodeParameters())
        issuer = a.info.chooseIdentity()
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

    private fun StartedMockNode.createAuction() : AuctionResponse {
        val assetFuture = this.startFlow(IssueAssetFlow("My asset"))
        mockNetwork.runNetwork()
        val assetId = assetFuture.getOrThrow().id

        val future = this.startFlow(CreateAuctionFlow(
                assetId,
                100.GBP issuedBy a.info.chooseIdentity(),
                listOf(b, c).map { it.info.chooseIdentityAndCert().party },
                100.GBP,
                10_000L))
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    private fun StartedMockNode.issueTokens(amount: Amount<TokenType>, to: Party): FungibleToken {
        val flow = IssueTokens(listOf(amount issuedBy issuer heldBy to))
        val future = this.startFlow(flow)
        mockNetwork.runNetwork()
        return future.getOrThrow() .tx.outputStates.single() as FungibleToken
    }

    @Test
    fun flowReturnsCorrectlyFormedSignedTx() {
        val auctionTx = a.createAuction().stx.tx
        val auction = auctionTx.outputsOfType<AuctionState>().single()
        val bidder = b.info.chooseIdentityAndCert().party
        a.issueTokens(auction.price.withoutIssuer(), bidder)
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
        val auctionTx = a.createAuction().stx.tx
        val auction = auctionTx.outputsOfType<AuctionState>().single()
        a.issueTokens(auction.price.withoutIssuer() - 10.GBP, b.info.chooseIdentity())
        val future = b.startFlow(BidAuctionFlow(auction.linearId))
        mockNetwork.runNetwork()
        assertFailsWith<IllegalStateException> { future.getOrThrow() }
    }

    @Test
    fun mustBeBidder() {
        val auctionTx = a.createAuction().stx.tx
        val auction = auctionTx.outputsOfType<AuctionState>().single()
        a.issueTokens(auction.price.withoutIssuer() - 10.GBP, a.info.chooseIdentity())
        val future = a.startFlow(BidAuctionFlow(auction.linearId))
        mockNetwork.runNetwork()
        assertFailsWith<NoSuchElementException> { future.getOrThrow() }
    }
}