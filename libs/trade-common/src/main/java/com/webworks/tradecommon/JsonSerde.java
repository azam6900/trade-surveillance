package com.webworks.tradecommon;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

public class JsonSerde<T> implements Serde<T> {

    private final Class<T> type;

    public JsonSerde(Class<T> type) {
        this.type = type;
    }

    @Override
    public Serializer<T> serializer() {
        return new Serializer<>() {
            @Override
            public byte[] serialize(String topic, T data) {
                return data == null ? null : Json.write(data).getBytes(StandardCharsets.UTF_8);
            }
        };
    }

    @Override
    public Deserializer<T> deserializer() {
        return new Deserializer<>() {
            @Override
            public T deserialize(String topic, byte[] bytes) {
                return bytes == null ? null : Json.read(bytes, type);
            }
        };
    }
}