package com.example.demo.handler;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.lang.reflect.Field;

@MappedTypes({Object.class})
@MappedJdbcTypes({JdbcType.VARCHAR})
public class JsonTypeHandler extends AbstractJsonTypeHandler<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    public JsonTypeHandler(Class<?> type) {
        super(type);
    }

    public JsonTypeHandler(Class<?> type, Field field) {
        super(type, field);
    }

    @SneakyThrows
    @Override
    public Object parse(String json) {
        if (json == null) {
            return null;
        }
        return OBJECT_MAPPER.readValue(json, new TypeReference<Object>() {});
    }

    @SneakyThrows
    @Override
    public String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return OBJECT_MAPPER.writeValueAsString(obj);
    }
}
