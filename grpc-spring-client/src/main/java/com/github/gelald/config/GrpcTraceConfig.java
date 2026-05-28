package com.github.gelald.config;

import com.github.gelald.grpc.interceptor.GrpcTraceClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.GlobalClientInterceptor;

/**
 * gRPC Trace 拦截器注册。
 * grpc-spring-client 仅作为客户端，因此只注册 ClientInterceptor。
 */
@Configuration(proxyBeanMethods = false)
public class GrpcTraceConfig {

    @Bean
    @Order(100)
    @GlobalClientInterceptor
    GrpcTraceClientInterceptor grpcTraceClientInterceptor() {
        return new GrpcTraceClientInterceptor();
    }
}
