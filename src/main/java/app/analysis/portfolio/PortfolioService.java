package app.analysis.portfolio;

import app.broker.BrokerEntityNotFoundException;
import app.broker.UniqueViolationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class PortfolioService {

    private final PortfolioRepository repository;
    private final PortfolioMapper mapper;

    public List<PortfolioDto> listPortfolios() {
        return mapper.toDto(repository.findAll());
    }

    public PortfolioDto addPortfolio(CreatePortfolioCommand command) {
        if (repository.countByName(command.getName()) == 0) {
            return mapper.toDto(
                    repository.save(new Portfolio(command.getName(), new HashSet<>(command.getAccountIds()))));
        }
        throw new UniqueViolationException("Another portfolio already exists with the name: " + command.getName());
    }

    @Transactional
    public void updatePortfolio(Long portfolioId, UpdatePortfolioCommand command) {
        Portfolio portfolio = repository.findById(portfolioId)
                .orElseThrow(() -> new BrokerEntityNotFoundException("No portfolio with name: " + portfolioId));
        if (!portfolio.getName().equals(command.getName()) && repository.countByName(command.getName()) == 1) {
            throw new UniqueViolationException("Another portfolio already exists with the name: " + command.getName());
        }
        portfolio.setName(command.getName());
        portfolio.setAccountIds(new HashSet<>(command.getAccountIds()));
    }

    public Set<Long> getAccountIdsInPortfolio(String portfolioName) {
        return repository.getPortfolioByName(portfolioName)
                .orElseThrow(() -> new BrokerEntityNotFoundException("No portfolio with name: " + portfolioName))
                .getAccountIds();
    }
}
