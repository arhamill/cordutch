package com.cordutch.server.config.jackson

import com.cordutch.server.NodeRPCConnection
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.bluebank.braid.corda.serialisation.AmountDeserializer
import io.bluebank.braid.corda.serialisation.AmountSerializer
import net.corda.client.jackson.JacksonSupport
import net.corda.core.contracts.Amount
import org.springframework.boot.jackson.JsonComponentModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.web.cors.reactive.CorsUtils
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import reactor.core.publisher.Mono
import org.springframework.web.server.WebFilterChain
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter


@Configuration
class JacksonConfiguration{

    @Bean
    fun rpcObjectMapper(rpc: NodeRPCConnection) = JacksonSupport.createDefaultMapper(rpc.proxy)
            .addMixIn(Amount::class.java, AmountMixin::class.java)

    @Bean
    fun decoder(rpcObjectMapper: ObjectMapper): Jackson2JsonDecoder =
            Jackson2JsonDecoder(rpcObjectMapper, MediaType.APPLICATION_JSON, MediaType.APPLICATION_STREAM_JSON)

    @JsonDeserialize(using = AmountDeserializer::class)
    @JsonSerialize(using = AmountSerializer::class)
    abstract class AmountMixin

}