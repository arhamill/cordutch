package com.template.states

import com.template.contracts.AuctionableAssetContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty

@BelongsToContract(AuctionableAssetContract::class)
data class AuctionableAsset(
        val description: String,
        val owner: AbstractParty,
        val issuer: AbstractParty,
        val locked: Boolean = false,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants: List<AbstractParty>
        get() = listOf(owner, issuer)

    fun withNewOwner(newOwner: AbstractParty) : AuctionableAsset {
        return copy(owner = newOwner)
    }
}