package com.cordutch

import com.cordutch.flows.BidAuctionFlow
import com.cordutch.flows.CreateAuctionFlow
import com.cordutch.flows.IssueAssetFlow
import com.cordutch.states.AuctionableAsset
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.money.GBP
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.Permissions.Companion.invokeRpc
import net.corda.node.services.Permissions.Companion.startFlow
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.core.CHARLIE_NAME
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.User
import net.corda.testing.node.internal.findCordapp
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals

class AuctionTests {

    @Test
    fun bidAuctionResolution() {
        val cordutchContracts = setOf(
                "com.cordutch.contracts",
                "com.cordutch.flows",
                "com.cordutch.services",
                "com.r3.corda.lib.tokens.contracts",
                "com.r3.corda.lib.tokens.workflows",
                "com.r3.corda.lib.tokens.money"
        ).map(::findCordapp)

        driver(DriverParameters(startNodesInProcess = true, cordappsForAllNodes = cordutchContracts)) {
            val aliceUser = User("aliceUser", "testPassword1", permissions = setOf(
                    startFlow<IssueTokens>(),
                    startFlow<IssueAssetFlow>(),
                    startFlow<CreateAuctionFlow>(),
                    invokeRpc("vaultQueryByCriteria")
            ))

            val bobUser = User("bobUser", "testPassword2", permissions = setOf(
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
            val aliceParty = alice.nodeInfo.chooseIdentity()
            val bobClient = CordaRPCClient(bob.rpcAddress)
            val bobProxy: CordaRPCOps = bobClient.start("bobUser", "testPassword2").proxy
            val bobParty = bob.nodeInfo.chooseIdentity()
            val assetId = aliceProxy.startFlow(::IssueAssetFlow, "My valuable asset").returnValue.getOrThrow().id
            val bidders = listOf(bob, charlie).map { it.nodeInfo.chooseIdentity() }
            val auctionId = aliceProxy.startFlow(
                    ::CreateAuctionFlow,
                    assetId,
                    1000.GBP issuedBy aliceParty,
                    bidders,
                    100.GBP,
                    30_000L,
                    Instant.now()).returnValue.getOrThrow().id
            aliceProxy.startFlow(::IssueTokens, listOf(1000.GBP issuedBy aliceParty heldBy bobParty), listOf()).returnValue.getOrThrow()
            bobProxy.startFlow(::BidAuctionFlow, auctionId).returnValue.getOrThrow()

            // Defaults to unconsumed states only
            val criteria = QueryCriteria.VaultQueryCriteria()
            val aliceAssets = aliceProxy.vaultQueryByCriteria(criteria, AuctionableAsset::class.java)
            val bobAssets = bobProxy.vaultQueryByCriteria(criteria, AuctionableAsset::class.java)
            val aliceCash = aliceProxy.vaultQueryByCriteria(criteria, FungibleToken::class.java)
            val bobCash = bobProxy.vaultQueryByCriteria(criteria, FungibleToken::class.java)

            assert(aliceAssets.states.isEmpty())
            assertEquals(1000.GBP issuedBy aliceParty, aliceCash.states.single().state.data.amount)
            assert(bobAssets.states.any { it.state.data.linearId == assetId })
            assert(bobCash.states.isEmpty())
        }
    }
}