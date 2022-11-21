package app.util;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.math.BigDecimal;
import java.math.MathContext;

/*
https://stackoverflow.com/questions/47703481/jpa-save-bigdecimal-as-integer-in-database
 */
@Converter
public class BigDecimalConverter implements AttributeConverter<BigDecimal, Long> {

    @Override
    public Long convertToDatabaseColumn(BigDecimal value) {
        if (value == null) {
            return null;
        } else {
            return value.multiply(BigDecimal.valueOf(1000000)).longValue();
        }
    }

    @Override
    public BigDecimal convertToEntityAttribute(Long value) {
        if (value == null) {
            return null;
        } else {
            return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(1000000), MathContext.DECIMAL64);
        }
    }
}