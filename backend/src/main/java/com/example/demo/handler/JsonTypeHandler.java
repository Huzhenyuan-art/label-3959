package com.example.demo.handler;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

@MappedTypes({Object.class})
@MappedJdbcTypes({JdbcType.VARCHAR})
public class JsonTypeHandler extends AbstractJsonTypeHandler<Object> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    @SneakyThrows
    @Override
    protected Object parse(String json) {
        if (json == null) {
            return null;
        }
        return OBJECT_MAPPER.readValue(json, new TypeReference<Object>() {});
    }

    @SneakyThrows
    @Override
    protected String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return OBJECT_MAPPER.writeValueAsString(obj);
    }
}
