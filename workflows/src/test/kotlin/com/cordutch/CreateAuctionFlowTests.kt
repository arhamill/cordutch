package com.cordutch

import com.cordutch.flows.AssetIssueFlow
import com.cordutch.flows.CreateAuctionFlow
import com.cordutch.flows.CreateAuctionResponderFlow
import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
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
import java.lang.IllegalArgumentException
import kotlin.test.assertFailsWith

class CreateAuctionFlowTests {
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
        startedNodes.forEach { it.registerInitiatedFlow(CreateAuctionResponderFlow::class.java) }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    private fun issueAsset(description: String) : SignedTransaction {
        val future = a.startFlow(AssetIssueFlow(description))
        mockNetwork.runNetwork()
        return future.getOrThrow()
    }

    @Test
    fun flowReturnsCorrectlyFormedSignedTx() {
        val owner = a.info.chooseIdentityAndCert().party
        val bidders = listOf(b, c).map { it.info.chooseIdentityAndCert().party }
        val issueTx = issueAsset("A big house")

        val asset = issueTx.tx.outputStates.single() as AuctionableAsset
        val future = a.startFlow(CreateAuctionFlow(asset.linearId, 100.POUNDS, bidders))
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()
        assert(stx.tx.inputs.single() == StateRef(issueTx.id, 0)) { "Should have asset tx as input" }
        assert(stx.tx.outputsOfType<AuctionableAsset>().single() == asset.lock()) { "Should lock input asset" }
        val auction = stx.tx.outputsOfType<AuctionState>().single()
        assert(auction.asset == asset.lock()) { "Auction should contain output asset" }
        assert(auction.bidders == bidders) { "Auction should contain correct bidders" }
        assert(auction.owner == owner) { "Auction owner should be initiator" }
        assert(auction.price == 100.POUNDS) { "Auction should have correct price" }
        stx.verifyRequiredSignatures()
    }

    @Test
    fun assetMustExist() {
        val bidders = listOf(b, c).map { it.info.chooseIdentityAndCert().party }
        val future = a.startFlow(CreateAuctionFlow(UniqueIdentifier(), 100.POUNDS, bidders))
        mockNetwork.runNetwork()
        assertFailsWith<IllegalArgumentException>("Should fail if asset doesn't exist") { future.getOrThrow() }
    }

    @Test
    fun priceMustBePositive() {
        val bidders = listOf(b, c).map { it.info.chooseIdentityAndCert().party }
        val issueTx = issueAsset("A big house")

        val asset = issueTx.tx.outputStates.single() as AuctionableAsset
        val future = a.startFlow(CreateAuctionFlow(asset.linearId, 0.POUNDS, bidders))
        mockNetwork.runNetwork()
        assertFailsWith<TransactionVerificationException>("Should fail if price is 0") { future.getOrThrow() }
    }

    @Test
    fun ownerMustNotBeBidder() {
        val bidders = listOf(a, b, c).map { it.info.chooseIdentityAndCert().party }
        val issueTx = issueAsset("A big house")

        val asset = issueTx.tx.outputStates.single() as AuctionableAsset
        val future = a.startFlow(CreateAuctionFlow(asset.linearId, 100.POUNDS, bidders))
        mockNetwork.runNetwork()
        assertFailsWith<TransactionVerificationException>("Should fail if owner is bidder") { future.getOrThrow() }
    }
}