package app.broker.product.commission;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductCommissionDto {
    private Long id;

    private String market;

    private BigDecimal percentFee;
    private BigDecimal minimumFee;
    private BigDecimal maximumFee;
    private String currency;

    private LocalDate fromDate;
    private LocalDate toDate;
}
