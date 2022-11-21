package app.data.fx.currency;

import java.util.List;
import java.util.Optional;

public interface CurrencyService {
    Optional<BasicCurrency> getByCode(String currencyCode);

    Optional<BasicCurrency> getById(Long currencyId);

    List<BasicCurrency> getAllById(List<Long> currencyIds);
}
