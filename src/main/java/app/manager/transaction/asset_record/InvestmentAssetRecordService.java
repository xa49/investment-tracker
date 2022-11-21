package app.manager.transaction.asset_record;

import app.data.DataService;
import app.data.fx.currency.BasicCurrency;
import app.data.securities.security.Security;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class InvestmentAssetRecordService {

    private final InvestmentAssetRecordRepository assetRecordRepository;
    private final DataService dataService;

    public InvestmentAssetRecord getCashRecord(String currencyCode) {
        BasicCurrency currencyDetails = dataService.getCurrencyDetailsByCode(currencyCode);

        Optional<InvestmentAssetRecord> existingRecord =
                assetRecordRepository.findCashRecordById(currencyDetails.getId());
        if (existingRecord.isPresent()) {
            return existingRecord.get();
        }
        InvestmentAssetRecord newRecord = new InvestmentAssetRecord(InvestmentAssetType.CASH, currencyDetails.getId());
        assetRecordRepository.save(newRecord);
        return newRecord;

    }

    public InvestmentAssetRecord getSecurityRecord(String ticker) {
        Security security = dataService.getSecurityDetailsByTicker(ticker);

        Optional<InvestmentAssetRecord> existingRecord = assetRecordRepository.findSecurityRecordById(security.getId());
        if (existingRecord.isPresent()) {
            return existingRecord.get();
        }
        InvestmentAssetRecord newRecord = new InvestmentAssetRecord(InvestmentAssetType.SECURITY, security.getId());
        assetRecordRepository.save(newRecord);
        return newRecord;
    }
}
