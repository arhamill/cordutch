package com.cordutch

import com.cordutch.flows.*
import com.cordutch.states.AuctionableAsset
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.SignedTransaction
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

class ConsumeAssetFlowTests {

    lateinit var mockNetwork: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @Before
    fun setup() {
        mockNetwork = MockNetwork(listOf("com.cordutch"),
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB"))))
        a = mockNetwork.createNode(MockNodeParameters())
        b = mockNetwork.createNode(MockNodeParameters())
        val startedNodes = arrayListOf(a, b)
        // For real nodes this happens automatically, but we have to manually register the flow for tests
        startedNodes.forEach {
            it.registerInitiatedFlow(TransferAssetResponderFlow::class.java)
            it.registerInitiatedFlow(ConsumeAssetResponderFlow::class.java)
        }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    private fun issueAndTransferAsset() : SignedTransaction {
        val issueFuture = a.startFlow(IssueAssetFlow("My asset"))
        mockNetwork.runNetwork()
        val assetId = issueFuture.getOrThrow().id

        val transferFuture = a.startFlow(TransferAssetFlow(assetId, b.info.chooseIdentityAndCert().party))
        mockNetwork.runNetwork()
        return transferFuture.getOrThrow()
    }

    @Test
    fun flowReturnsCorrectlyFormedSignedTx() {
        val assetTx = issueAndTransferAsset().tx
        val asset = assetTx.outputsOfType<AuctionableAsset>().single()

        val future = b.startFlow(ConsumeAssetFlow(asset.linearId))
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()

        assert(stx.tx.inputs.single() == assetTx.outRef<AuctionableAsset>(0).ref)
        assert(stx.tx.outputs.isEmpty())
        stx.verifyRequiredSignatures()
    }

    @Test
    fun mustBeExistingAsset() {
        val future = a.startFlow(ConsumeAssetFlow(UniqueIdentifier()))
        mockNetwork.runNetwork()
        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }
    }
}