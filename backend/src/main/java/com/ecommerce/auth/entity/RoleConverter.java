package com.ecommerce.auth.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter that maps Java Role enums (e.g. ROLE_USER)
 * to database column values (e.g. USER) matching PostgreSQL CHECK constraints.
 */
@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        return role != null ? role.getDbValue() : null;
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        return dbData != null ? Role.fromDbValue(dbData) : null;
    }
}
