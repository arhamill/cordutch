package com.cordutch

import com.cordutch.flows.AssetIssueFlow
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

class AssetIssueFlowTests {
    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(listOf("com.cordutch"),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
        a = mockNetwork.createNode(MockNodeParameters())
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    @Test
    fun flowCreatesAssetAndStoresInVault() {
        val issuer = a.info.chooseIdentityAndCert().party
        val description = "A big house"
        val flow = AssetIssueFlow(description)
        val future = a.startFlow(flow)
        val stx = future.getOrThrow()
        stx.verifyRequiredSignatures()
        val output = stx.coreTransaction.outputStates.single() as AuctionableAsset
        assert(issuer == output.issuer) { "Initiator should be issuer" }
        assert(issuer == output.owner) { "Initiator should be owner" }
        assert(a.services.validatedTransactions.getTransaction(stx.id) != null) { "Transaction must be stored in vault" }
    }

    @Test
    fun flowFailsWithEmptyDesc() {
        val flow = AssetIssueFlow("")
        val future = a.startFlow(flow)
        assertFailsWith<TransactionVerificationException> ("Should fail contract verification if description is empty"){ future.getOrThrow() }
    }
}