package com.cordutch.services

import com.cordutch.flows.IssueAssetFlow
import com.cordutch.states.AuctionResponse
import com.cordutch.states.AuctionState
import com.cordutch.states.AuctionableAsset
import com.r3.corda.lib.tokens.contracts.utilities.withoutIssuer
import io.bluebank.braid.corda.BraidConfig
import io.vertx.core.http.HttpServerOptions
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.SingletonSerializeAsToken
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@CordaService
class BraidService(serviceHub: AppServiceHub) : SingletonSerializeAsToken() {
    init {
        val portExt = serviceHub.myInfo.addresses.first().port % 10
        BraidConfig()
                .withFlow(IssueAssetFlow::class.java)
                .withService("assets", AssetService(serviceHub))
                .withPort(8080 + portExt)
                .withHttpServerOptions(HttpServerOptions().setSsl(false))
                .bootstrapBraid(serviceHub)
    }
}

class AssetService(private val serviceHub: AppServiceHub) {
    fun getAssets() = serviceHub.vaultService.queryBy(AuctionableAsset::class.java).states.map { it.state.data }

    fun getAuctions(): List<AuctionUI> {
        val auctions = serviceHub.vaultService.queryBy(AuctionState::class.java).states.map { it.state.data }

        return auctions.map {
            val timeNow = Instant.now()
            val periods = (timeNow.toEpochMilli() - it.startTime.toEpochMilli()) / it.period
            val priceNow = it.price.withoutIssuer() - (it.decrement * periods)

            val ownerName = serviceHub.identityService.requireWellKnownPartyFromAnonymous(it.owner).name.toString()

            AuctionUI(
                    it.linearId.id,
                    ownerName,
                    priceNow.toDecimal()
            )
        }
    }

    fun getAsset(assetId: String): AuctionableAsset {
        val id = UniqueIdentifier(id = UUID.fromString(assetId))
        val crit = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
        return serviceHub.vaultService.queryBy<AuctionableAsset>(crit).states.single().state.data
    }
}

data class AuctionUI(val id: UUID, val owner: String, val price: BigDecimal)