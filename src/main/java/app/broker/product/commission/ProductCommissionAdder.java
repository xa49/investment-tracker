package app.broker.product.commission;

import app.broker.BrokerAdderUpdaterTemplate;
import app.broker.product.BrokerProduct;
import app.broker.product.BrokerProductRepository;
import app.broker.CommandLoadable;
import app.util.DateRange;
import app.broker.RequestCommand;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProductCommissionAdder extends BrokerAdderUpdaterTemplate {
    private final BrokerProductRepository productRepository;
    private final ProductCommissionRepository commissionRepository;

    public ProductCommissionAdder(
            BrokerProductRepository productRepository, ProductCommissionRepository commissionRepository) {
        super(productRepository, "BrokerProduct", commissionRepository, "BrokerProductCommission");
        this.productRepository = productRepository;
        this.commissionRepository = commissionRepository;
    }

    @Override
    protected int getAssociationCount(Long parentId, Long itemId) {
        return commissionRepository.countByProductIdAndId(parentId, itemId);
    }

    @Override
    protected List<DateRange> getOverlappingPeriods(
            Long parentId, LocalDate from, LocalDate to, RequestCommand command) {
        return commissionRepository.getOverlappingDates(parentId, ((CommissionCommand) command).getMarket(), from, to);
    }

    @Override
    protected List<DateRange> getOtherOverlappingPeriods(
            Long parentId, Long itemId, LocalDate from, LocalDate to, RequestCommand command) {
        return commissionRepository.getOtherOverlappingDates(parentId, itemId,
                ((CommissionCommand) command).getMarket(), from, to);
    }

    @Override
    protected CommandLoadable getEmptyLoadableItem() {
        return new ProductCommission();
    }

    @Override
    protected void setParentReference(
            CommandLoadable item, Long parentId, LocalDate from, LocalDate to, RequestCommand command) {
        BrokerProduct product = productRepository.getReferenceById(parentId);
        ((ProductCommission) item).setProduct(product);
    }
}
