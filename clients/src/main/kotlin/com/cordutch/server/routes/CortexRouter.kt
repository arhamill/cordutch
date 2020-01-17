package com.cordutch.server.routes

import com.cordutch.server.handlers.CortexHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class CortexRouter {
    @Bean
    fun routes(handler: CortexHandler): RouterFunction<ServerResponse> = router {
        ("/updates" and accept(MediaType.TEXT_EVENT_STREAM)).nest {
            POST("/", handler::updates)
        }
        ("/tokens" and accept(MediaType.TEXT_EVENT_STREAM)).nest {
            GET("/", handler::tokens)
        }
    }
}