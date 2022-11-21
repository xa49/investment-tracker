package app.broker.product.commission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidCommission
public class CreateCommissionCommand  implements CommissionCommand {
    private String market;

    private BigDecimal percentFee;
    private BigDecimal minimumFee;
    private BigDecimal maximumFee;
    private String currency;

    private LocalDate fromDate;
    private LocalDate toDate;
}
