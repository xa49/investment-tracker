package app.data.fx.currency;

import app.data.fx.currency.full_names.CurrencyNameService;
import app.data.fx.mnb_access.MNBCurrency;
import app.data.fx.mnb_access.MNBQueryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class CurrencyServiceBasic implements CurrencyService {
    private static final String MISSING_FULL_NAME_PLACEHOLDER = "";

    private final BasicCurrencyRepository currencyRepository;
    private final MNBQueryService mnbQueryService;

    private final CurrencyNameService currencyNameService;

    @Override
    public Optional<BasicCurrency> getByCode(String currencyCode) {
        Optional<BasicCurrency> localCurrency = currencyRepository.findByIsoCode(currencyCode);
        return localCurrency.isPresent() ? localCurrency : currencyFromMNB(currencyCode);
    }

    @Override
    public Optional<BasicCurrency> getById(Long currencyId) {
        return currencyRepository.findById(currencyId);
    }

    @Override
    public List<BasicCurrency> getAllById(List<Long> currencyIds) {
        return currencyRepository.findAllById(currencyIds);
    }

    private Optional<BasicCurrency> currencyFromMNB(String currencyCode) {
        log.info("Requesting currency detail from MNB for {}", currencyCode);
        Optional<MNBCurrency> mnbCurrency = mnbQueryService.getCurrencyDetails(currencyCode);
        if (mnbCurrency.isPresent()) {
            BasicCurrency newCurrency =
                    new BasicCurrency(mnbCurrency.get().getCurrency(), getCurrencyName(currencyCode));
            currencyRepository.save(newCurrency);
            return Optional.of(newCurrency);
        } else {
            return Optional.empty();
        }
    }

    private String getCurrencyName(String currencyCode) {
        try {
            return currencyNameService.getCurrencyName(currencyCode);
        } catch (IllegalArgumentException e) {
            return MISSING_FULL_NAME_PLACEHOLDER;
        }
    }
}
