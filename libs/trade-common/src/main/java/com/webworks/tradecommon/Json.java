package com.webworks.tradecommon;

import tools.jackson.databind.json.JsonMapper;

public final class Json {   // Jackson 3: package tools.jackson, exceptions are unchecked
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    public static String write(Object o) { return MAPPER.writeValueAsString(o); }
    public static <T> T read(String s, Class<T> t) { return MAPPER.readValue(s, t); }
    public static <T> T read(byte[] b, Class<T> t) { return MAPPER.readValue(b, t); }
}