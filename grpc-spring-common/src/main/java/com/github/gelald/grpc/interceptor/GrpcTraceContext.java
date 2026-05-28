package com.github.gelald.grpc.interceptor;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 全链路追踪上下文，基于 ThreadLocal 存储当前线程的 trace 信息。
 *
 * <p>注意：common 模块不持有完整 Spring 上下文，因此这里只做数据容器，
 * 不负责 Bean 注册。注册交给各应用模块的 {@code @Configuration} 类完成。
 */
public final class GrpcTraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> SPAN_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> PARENT_SPAN_ID = new ThreadLocal<>();

    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    private static final String MDC_PARENT_SPAN_ID = "parentSpanId";

    private GrpcTraceContext() {
    }

    static String generateId() {
        return Long.toHexString(UUID.randomUUID().getMostSignificantBits());
    }

    public static void set(String traceId, String spanId, String parentSpanId) {
        TRACE_ID.set(traceId);
        SPAN_ID.set(spanId);
        PARENT_SPAN_ID.set(parentSpanId);
    }

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    public static String getSpanId() {
        return SPAN_ID.get();
    }

    public static String getParentSpanId() {
        return PARENT_SPAN_ID.get();
    }

    /**
     * 将当前 trace 信息写入 SLF4J MDC，使日志自动携带 trace 标识。
     */
    public static void putToMdc() {
        String traceId = TRACE_ID.get();
        String spanId = SPAN_ID.get();
        String parentSpanId = PARENT_SPAN_ID.get();
        if (traceId != null) {
            MDC.put(MDC_TRACE_ID, traceId);
        }
        if (spanId != null) {
            MDC.put(MDC_SPAN_ID, spanId);
        }
        if (parentSpanId != null && !parentSpanId.isEmpty()) {
            MDC.put(MDC_PARENT_SPAN_ID, parentSpanId);
        }
    }

    public static void clear() {
        TRACE_ID.remove();
        SPAN_ID.remove();
        PARENT_SPAN_ID.remove();
    }

    public static void clearMdc() {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_SPAN_ID);
        MDC.remove(MDC_PARENT_SPAN_ID);
    }
}
