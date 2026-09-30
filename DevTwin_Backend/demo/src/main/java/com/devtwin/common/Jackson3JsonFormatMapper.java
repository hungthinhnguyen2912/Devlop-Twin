package com.devtwin.common;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class Jackson3JsonFormatMapper implements FormatMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <T> T fromString(CharSequence value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        try {
            if (JsonNode.class.isAssignableFrom(javaType.getJavaTypeClass())) {
                return javaType.cast(objectMapper.readTree(value.toString()));
            }
            return objectMapper.readValue(value.toString(), javaType.getJavaTypeClass());
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Could not deserialize JSON to " + javaType.getTypeName(),
                    exception
            );
        }
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Could not serialize " + javaType.getTypeName() + " to JSON",
                    exception
            );
        }
    }
}
