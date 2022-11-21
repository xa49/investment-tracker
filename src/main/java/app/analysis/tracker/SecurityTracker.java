package app.analysis.tracker;

import app.analysis.actual.SecurityPosition;
import app.broker.account.BrokerAccount;
import app.manager.transaction.MatchingStrategy;
import app.data.securities.security.Security;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.*;

@NoArgsConstructor
public class SecurityTracker {

    private Map<BrokerAccount, Map<Security, List<SecurityPosition>>> securityPositions = new HashMap<>();

    private SecurityTracker(Map<BrokerAccount, Map<Security, List<SecurityPosition>>> securityPositions) {
        this.securityPositions = securityPositions;
    }

    public SecurityTracker copy() {
        Map<BrokerAccount, Map<Security, List<SecurityPosition>>> balances = new HashMap<>();
        for (Map.Entry<BrokerAccount, Map<Security, List<SecurityPosition>>> entry : securityPositions.entrySet()) {
            balances.put(entry.getKey(), new HashMap<>());

            for (Map.Entry<Security, List<SecurityPosition>> securityEntry : entry.getValue().entrySet()) {
                balances.get(entry.getKey()).put(securityEntry.getKey(), new ArrayList<>());

                securityEntry.getValue().forEach(
                        sp -> balances.get(entry.getKey()).get(securityEntry.getKey())
                                .add(new SecurityPosition(sp.getSecurity(), sp.getCount(),
                                        sp.getEnteredAt(), sp.getEnterDate()))
                );
            }
        }
        return new SecurityTracker(balances);
    }

    public Map<BrokerAccount, Map<Security, List<SecurityPosition>>> getSecurityPositions() {
        return Collections.unmodifiableMap(securityPositions);
    }

    public void addPosition(BrokerAccount account, SecurityPosition securityPosition) {
        ensureBucketExists(account, securityPosition.getSecurity());
        securityPositions.get(account).get(securityPosition.getSecurity()).add(securityPosition);
    }

    public List<SecurityPosition> closePositions(BrokerAccount account, Security security, BigDecimal countToClose,
                                                 MatchingStrategy matchingStrategy) {
        ensureBucketExists(account, security);

        List<SecurityPosition> availablePositions = securityPositions.get(account)
                .getOrDefault(security, new ArrayList<>());

        return getClosedPositions(security, countToClose, availablePositions, matchingStrategy);
    }

    public void transferPositions(BrokerAccount fromAccount, BrokerAccount toAccount, Security security,
                                  BigDecimal count, MatchingStrategy matchingStrategy) {
        List<SecurityPosition> transferredPositions = closePositions(fromAccount, security, count, matchingStrategy);

        ensureBucketExists(toAccount, security);
        securityPositions.get(toAccount).get(security).addAll(transferredPositions);
    }

    public void mergeAccounts(BrokerAccount mergeFrom, BrokerAccount mergeTo) {
        if (!mergeTo.equals(mergeFrom) && securityPositions.get(mergeFrom) != null) {
            securityPositions.get(mergeFrom).forEach(
                    (security, positions) -> {
                        ensureBucketExists(mergeTo, security);
                        securityPositions.get(mergeTo).merge(
                                security, positions, (to, from) -> {
                                    to.addAll(from);
                                    return to;
                                });
                    }
            );
            securityPositions.remove(mergeFrom);
        }
    }

    private List<SecurityPosition> getClosedPositions(Security security, BigDecimal countToClose,
                                                      List<SecurityPosition> availablePositions,
                                                      MatchingStrategy matchingStrategy) {
        matchingStrategy.sortSecurityPositions(availablePositions);

        List<SecurityPosition> closedPositions = new ArrayList<>();
        Iterator<SecurityPosition> it = availablePositions.iterator();
        BigDecimal remainingToClose = countToClose;
        while (it.hasNext() && remainingToClose.compareTo(BigDecimal.ZERO) > 0) {
            SecurityPosition current = it.next();
            if (current.getCount().compareTo(remainingToClose) <= 0) {
                closedPositions.add(current);
                remainingToClose = remainingToClose.subtract(current.getCount());
                it.remove();
            } else {
                SecurityPosition closedPart = new SecurityPosition(security, remainingToClose, current.getEnteredAt(),
                        current.getEnterDate());
                closedPositions.add(closedPart);
                current.setCount(current.getCount().subtract(remainingToClose));
                remainingToClose = BigDecimal.ZERO;
            }
        }
        if (remainingToClose.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Could not find enough securities to sell: " + security);
        }
        return closedPositions;
    }

    private void ensureBucketExists(BrokerAccount account, Security security) {
        securityPositions.putIfAbsent(account, new HashMap<>());
        securityPositions.get(account).putIfAbsent(security, new ArrayList<>());
    }
}
