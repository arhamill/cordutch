package com.cordutch

import com.cordutch.flows.*
import com.cordutch.states.AuctionableAsset
import com.cordutch.states.TransactionAndStateId
import net.corda.core.contracts.UniqueIdentifier
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
import java.lang.IllegalArgumentException
import kotlin.test.assertFailsWith

class TransferAssetFlowTests {
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
        }
        mockNetwork.runNetwork()
    }

    @After
    fun tearDown() {
        mockNetwork.stopNodes()
    }

    private fun issueAsset() : TransactionAndStateId {
        val assetFuture = a.startFlow(IssueAssetFlow("My asset"))
        mockNetwork.runNetwork()
        return assetFuture.getOrThrow()
    }

    @Test
    fun flowReturnsCorrectlyFormedSignedTx() {
        val assetTx = issueAsset().stx.tx
        val asset = assetTx.outputsOfType<AuctionableAsset>().single()
        val newOwner = b.info.chooseIdentityAndCert().party

        val future = a.startFlow(TransferAssetFlow(asset.linearId, newOwner))
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()

        assert(stx.tx.inputs.single() == assetTx.outRef<AuctionableAsset>(0).ref)
        assert(stx.tx.outputStates.single() as AuctionableAsset == asset.withNewOwner(newOwner))
        stx.verifyRequiredSignatures()
    }

    @Test
    fun mustBeExistingAssetInVault() {
        val future = a.startFlow(TransferAssetFlow(UniqueIdentifier(), b.info.chooseIdentityAndCert().party))
        mockNetwork.runNetwork()
        assertFailsWith<IllegalArgumentException> { future.getOrThrow() }
    }
}