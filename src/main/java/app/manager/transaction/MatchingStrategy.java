package app.manager.transaction;

import app.analysis.actual.SecurityPosition;

import java.util.Comparator;
import java.util.List;

public enum MatchingStrategy {
    FIFO {
        @Override
        public void sortSecurityPositions(List<SecurityPosition> securityPositions) {
            securityPositions.sort(Comparator.comparing(SecurityPosition::getEnterDate));
        }
    }, LIFO {
        @Override
        public void sortSecurityPositions(List<SecurityPosition> securityPositions) {
            securityPositions.sort(Comparator.comparing(SecurityPosition::getEnterDate).reversed());
        }
    };

    public abstract void sortSecurityPositions(List<SecurityPosition> securityPositions);
}
