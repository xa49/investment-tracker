package app.broker.account;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class AccountHistoryDto {
    private String accountName;
    private List<AssociationDetailDto> history;


    @Getter
    @AllArgsConstructor
    static class AssociationDetailDto {
        private Long associationId;
        private String productName;
        private String brokerName;
        private LocalDate fromDate;
        private LocalDate toDate;
    }
}
