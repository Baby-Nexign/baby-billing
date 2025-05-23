package org.babynexign.babybilling.commutatorservice.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.babynexign.babybilling.commutatorservice.entity.enums.CallType;

/**
 * A JPA converter for the CallType enum that automatically applies to entities using the CallType type.
 * This converter handles the transformation between CallType enum values and their String representations
 * in the database, storing the enum's index value as a string in the database column.
 */
@Converter(autoApply = true)
public class CallTypeConverter implements AttributeConverter<CallType, String> {

    @Override
    public String convertToDatabaseColumn(CallType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getIndex();
    }

    @Override
    public CallType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return switch (dbData) {
            case "01" -> CallType.OUTCOMING;
            case "02" -> CallType.INCOMING;
            default -> throw new IllegalArgumentException("Unknown database value: " + dbData);
        };
    }
}
