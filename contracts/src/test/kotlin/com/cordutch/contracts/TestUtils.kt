package com.cordutch.contracts

import net.corda.core.contracts.Amount
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.finance.`issued by`
import net.corda.finance.contracts.asset.Cash
import net.corda.testing.core.TestIdentity
import java.util.*

val ALICE = TestIdentity(CordaX500Name(organisation = "Alice", locality = "TestLand", country = "US"))
val BOB = TestIdentity(CordaX500Name(organisation = "Bob", locality = "TestCity", country = "US"))
val CHARLIE = TestIdentity(CordaX500Name(organisation = "Charlie", locality = "TestVillage", country = "US"))
val MINICORP = TestIdentity(CordaX500Name(organisation = "MiniCorp", locality = "MiniLand", country = "US"))
val MEGACORP = TestIdentity(CordaX500Name(organisation = "MegaCorp", locality = "MiniLand", country = "US"))

fun createCashState(amount: Amount<Currency>, owner: AbstractParty): Cash.State {
    val defaultRef = ByteArray(1) { 1 }
    return Cash.State(amount = amount `issued by` MEGACORP.ref(defaultRef.first()), owner = owner)
}
