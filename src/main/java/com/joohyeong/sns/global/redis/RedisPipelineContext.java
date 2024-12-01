package com.joohyeong.sns.global.redis;

import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.connection.StringRedisConnection;

@Log4j2
public class RedisPipelineContext {
    private static final ThreadLocal<StringRedisConnection> connectionHolder =
            ThreadLocal.withInitial(() -> null);

    public static void setConnection(StringRedisConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        connectionHolder.set(connection);
    }

    public static StringRedisConnection getConnection() {
        StringRedisConnection connection = connectionHolder.get();
        if (connection == null) {
            throw new IllegalStateException("Redis connection not initialized in current thread");
        }
        return connection;
    }

    public static void clear() {
        connectionHolder.remove();
    }
}