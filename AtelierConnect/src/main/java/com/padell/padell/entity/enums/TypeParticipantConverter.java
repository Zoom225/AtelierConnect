package com.padell.padell.entity.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TypeParticipantConverter implements AttributeConverter<TypeParticipant, String> {
    @Override
    public String convertToDatabaseColumn(TypeParticipant category) {
        return category == null ? null : category.toLegacyType().name();
    }

    @Override
    public TypeParticipant convertToEntityAttribute(String databaseValue) {
        return TypeParticipant.fromDatabaseValue(databaseValue);
    }
}
