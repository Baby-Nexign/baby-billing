package org.babynexign.babybilling.brtservice.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.babynexign.babybilling.brtservice.entity.enums.RecordType;

@Converter(autoApply = true)
public class RecordTypeConverter implements AttributeConverter<RecordType, String> {

    @Override
    public String convertToDatabaseColumn(RecordType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getIndex();
    }

    @Override
    public RecordType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return switch (dbData) {
            case "01" -> RecordType.OUTCOMING;
            case "02" -> RecordType.INCOMING;
            default -> throw new IllegalArgumentException("Unknown database value: " + dbData);
        };
    }
}
