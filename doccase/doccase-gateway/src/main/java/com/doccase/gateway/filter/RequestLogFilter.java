package com.doccase.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long startTime = System.currentTimeMillis();

        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Request-Id", requestId)
                .build();

        log.info("[{}] {} {} from {}", requestId, request.getMethod(), request.getURI().getPath(),
                request.getRemoteAddress());

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[{}] completed in {}ms with status {}", requestId, duration,
                            exchange.getResponse().getStatusCode());
                }));
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
