package com.github.gelald.config;

import com.github.gelald.grpc.interceptor.GrpcTraceServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;

/**
 * gRPC Trace 拦截器注册。
 * downstream-grpc-server 仅作为服务端，因此只注册 ServerInterceptor。
 */
@Configuration(proxyBeanMethods = false)
public class GrpcTraceConfig {

    @Bean
    @Order(100)
    @GlobalServerInterceptor
    GrpcTraceServerInterceptor grpcTraceServerInterceptor() {
        return new GrpcTraceServerInterceptor();
    }
}
