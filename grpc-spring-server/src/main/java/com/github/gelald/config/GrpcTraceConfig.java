package com.github.gelald.config;

import com.github.gelald.grpc.interceptor.GrpcTraceClientInterceptor;
import com.github.gelald.grpc.interceptor.GrpcTraceServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.grpc.server.GlobalServerInterceptor;

/**
 * gRPC Trace 拦截器注册。
 * grpc-spring-server 同时作为服务端和客户端（调用 downstream），因此注册两组拦截器。
 */
@Configuration(proxyBeanMethods = false)
public class GrpcTraceConfig {

    @Bean
    @Order(100)
    @GlobalServerInterceptor
    GrpcTraceServerInterceptor grpcTraceServerInterceptor() {
        return new GrpcTraceServerInterceptor();
    }

    @Bean
    @Order(100)
    @GlobalClientInterceptor
    GrpcTraceClientInterceptor grpcTraceClientInterceptor() {
        return new GrpcTraceClientInterceptor();
    }
}
