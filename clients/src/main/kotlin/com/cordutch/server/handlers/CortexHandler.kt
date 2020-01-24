package com.cordutch.server.handlers

import com.cordutch.server.NodeRPCConnection
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.sumTokenStateAndRefsOrZero
import com.r3.corda.lib.tokens.contracts.utilities.sumTokenStatesOrZero
import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import rx.RxReactiveStreams.toPublisher
import rx.Single
import java.math.BigDecimal

@Component
class CortexHandler(rpc: NodeRPCConnection) {

    private val proxy = rpc.proxy

    fun snapshot(request: ServerRequest): Mono<ServerResponse> = request.bodyToMono(String::class.java).flatMap {
        val clazz = Class.forName(it).asSubclass(ContractState::class.java)
        ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toPublisher(Single.just(proxy.vaultQuery(clazz).states.groupStates())),
                        ParameterizedTypeReference.forType(Map::class.java))
    }

    fun updates(request: ServerRequest): Mono<ServerResponse> = request.bodyToMono(String::class.java).flatMap {
        val clazz = Class.forName(it).asSubclass(ContractState::class.java)
        ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(toPublisher(
                        proxy.vaultTrackByCriteria(clazz, QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.ALL)).updates.map { update ->
                            StateUpdate(
                                    consumed = update.consumed.groupStates(),
                                    produced = update.produced.groupStates()
                            )
                        }),
                        ParameterizedTypeReference.forType(StateUpdate::class.java))
    }

    fun tokenSnapshot(request: ServerRequest): Mono<ServerResponse> = ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(toPublisher(Single.just(proxy.vaultQuery(FungibleToken::class.java).states.sumTokens())),
                        ParameterizedTypeReference.forType(Map::class.java))

    fun tokenUpdates(request: ServerRequest): Mono<ServerResponse> = ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(
                    toPublisher(proxy.vaultTrackByCriteria(FungibleToken::class.java, QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.ALL)).updates.map {
                        TokenUpdate(
                                consumed = it.consumed.sumTokens(),
                                produced = it.produced.sumTokens()
                        )
                    }), ParameterizedTypeReference.forType(TokenUpdate::class.java)
            )

    fun Collection<StateAndRef<FungibleToken>>.sumTokens() = map { it.state.data }
            .groupBy { it.issuedTokenType }.mapValues { it.value.sumTokenStatesOrZero(it.key) }

    fun Collection<StateAndRef<ContractState>>.groupStates() = map { it.ref to it.state.data }.toMap()
}

class TokenUpdate(val consumed: Map<IssuedTokenType, Amount<IssuedTokenType>>, val produced: Map<IssuedTokenType, Amount<IssuedTokenType>>)

class StateUpdate(val consumed: Map<StateRef, ContractState>, val produced: Map<StateRef, ContractState>)