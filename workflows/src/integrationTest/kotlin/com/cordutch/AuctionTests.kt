package com.cordutch

import com.cordutch.flows.BidAuctionFlow
import com.cordutch.flows.CreateAuctionFlow
import com.cordutch.flows.IssueAssetFlow
import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.finance.POUNDS
import net.corda.finance.`issued by`
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueAndPaymentFlow
import net.corda.finance.flows.CashIssueFlow
import net.corda.node.services.Permissions.Companion.invokeRpc
import net.corda.node.services.Permissions.Companion.startFlow
import net.corda.testing.core.*
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.User
import net.corda.testing.node.internal.FINANCE_CORDAPPS
import net.corda.testing.node.internal.findCordapp
import org.junit.Test
import rx.Observable
import java.util.concurrent.Future
import javax.management.Query
import kotlin.test.assertEquals

class AuctionTests {

    @Test
    fun bidAuctionResolution() {
        val cordutchContracts = FINANCE_CORDAPPS + setOf(findCordapp("com.cordutch.contracts"), findCordapp("com.cordutch.flows"))
        driver(DriverParameters(startNodesInProcess = true, cordappsForAllNodes = cordutchContracts)) {
            val aliceUser = User("aliceUser", "testPassword1", permissions = setOf(
                    startFlow<IssueAssetFlow>(),
                    startFlow<CreateAuctionFlow>(),
                    invokeRpc("vaultQueryByCriteria")
            ))

            val bobUser = User("bobUser", "testPassword2", permissions = setOf(
                    startFlow<CashIssueFlow>(),
                    startFlow<BidAuctionFlow>(),
                    invokeRpc("vaultQueryByCriteria")
            ))

            val charlieUser = User("charlieUser", "testPassword3", permissions = setOf(
                    invokeRpc("vaultQueryByCriteria")
            ))

            val (alice, bob, charlie) = listOf(
                    startNode(providedName = ALICE_NAME, rpcUsers = listOf(aliceUser)),
                    startNode(providedName = BOB_NAME, rpcUsers = listOf(bobUser)),
                    startNode(providedName = CHARLIE_NAME, rpcUsers = listOf(charlieUser))
            ).map { it.getOrThrow() }

            val aliceClient = CordaRPCClient(alice.rpcAddress)
            val aliceProxy: CordaRPCOps = aliceClient.start("aliceUser", "testPassword1").proxy
            val bobClient = CordaRPCClient(bob.rpcAddress)
            val bobProxy: CordaRPCOps = bobClient.start("bobUser", "testPassword2").proxy
            val bobParty = bob.nodeInfo.chooseIdentity()
            val bobRef = bobParty.ref(OpaqueBytes.of(0))

            val asset = aliceProxy.startFlow(::IssueAssetFlow, "My valuable asset").returnValue.getOrThrow().stx.tx.outputStates.single() as AuctionableAsset
            val bidders = listOf(bob, charlie).map { it.nodeInfo.chooseIdentity() }
            val auctionId = aliceProxy.startFlow(::CreateAuctionFlow, asset.linearId, 1000.POUNDS, bidders).returnValue.getOrThrow().id
            bobProxy.startFlow(::CashIssueFlow, 1000.POUNDS, OpaqueBytes.of(0), defaultNotaryIdentity).returnValue.getOrThrow()
            bobProxy.startFlow(::BidAuctionFlow, auctionId).returnValue.getOrThrow()

            // Defaults to unconsumed states only
            val criteria = QueryCriteria.VaultQueryCriteria()
            val aliceAssets = aliceProxy.vaultQueryByCriteria(criteria, AuctionableAsset::class.java)
            val bobAssets = bobProxy.vaultQueryByCriteria(criteria, AuctionableAsset::class.java)
            val aliceCash = aliceProxy.vaultQueryByCriteria(criteria, Cash.State::class.java)
            val bobCash = bobProxy.vaultQueryByCriteria(criteria, Cash.State::class.java)

            assert(aliceAssets.states.isEmpty())
            assertEquals(asset.withNewOwner(bobParty), bobAssets.states.single().state.data)
            assertEquals(1000.POUNDS `issued by` bobRef, aliceCash.states.single().state.data.amount)
            assert(bobCash.states.isEmpty())
        }
    }
}