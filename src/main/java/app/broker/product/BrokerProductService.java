package app.broker.product;

import app.broker.BrokerMapper;
import app.broker.product.commission.*;
import app.broker.BrokerEntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class BrokerProductService {

    private BrokerProductRepository productRepository;

    private BrokerProductAdder productAdder;
    private ProductCommissionRepository commissionRepository;
    private ProductCommissionAdder commissionAdder;
    private BrokerMapper brokerMapper;
    private final BrokerProductMapper productMapper;
    private final ProductCommissionMapper commissionMapper;

    public List<BrokerProductDto> listProductsAtBroker(Long brokerId) {
        return productMapper.toDto(productRepository.findAllByBrokerId(brokerId));
    }

    public BrokerProductDto addProduct(Long brokerId, CreateBrokerProductCommand command) {
        return brokerMapper.toDto(
                (BrokerProduct) productAdder.addItem(brokerId, command.getFromDate(), command.getToDate(), command));
    }

    @Transactional
    public BrokerProductDto updateProduct(Long brokerId, Long productId, UpdateBrokerProductCommand command) {
        return brokerMapper.toDto(
                (BrokerProduct) productAdder.updateItem(brokerId, productId, command.getFromDate(), command.getToDate(), command));
    }

    public BrokerProductDto getProductById(Long productId) { // test only
        Optional<BrokerProduct> product = productRepository.findById(productId);
        if (product.isPresent()) {
            return brokerMapper.toDto(product.get());
        } else {
            throw new BrokerEntityNotFoundException("BrokerProduct not found with id: " + productId);
        }
    }

    public ProductCommissionDto addCommission(Long productId, CreateCommissionCommand command) {
        return brokerMapper.toDto((ProductCommission) commissionAdder.addItem(productId, command.getFromDate(),
                command.getToDate(), command));
    }

    @Transactional
    public ProductCommissionDto updateCommission(Long productId, Long commissionId, UpdateCommissionCommand command) {
        return brokerMapper.toDto((ProductCommission) commissionAdder.updateItem(productId, commissionId,
                command.getFromDate(), command.getToDate(), command));
    }

    public ProductCommissionDto getCommission(Long productId, String market, LocalDate date) {
        Optional<ProductCommission> commission =
                commissionRepository.getForProductAndMarketAndDate(productId, market, date);
        if (commission.isPresent()) {
            return brokerMapper.toDto(commission.get());
        } else {
            throw new BrokerEntityNotFoundException("ProductCommission not found for product: "
                    + productId + " and market: " + market + " date: " + date);
        }
    }

    public ProductCommissionDto getCommissionById(Long commissionId) {
        Optional<ProductCommission> commission = commissionRepository.findById(commissionId);
        if (commission.isPresent()) {
            return brokerMapper.toDto(commission.get());
        } else {
            throw new BrokerEntityNotFoundException("ProductCommission not found with id: " + commissionId);
        }
    }

    public List<ProductCommissionDto> listCommissionsForProduct(Long productId) {
        return commissionMapper.toDto(commissionRepository.findAllByProductId(productId));
    }
}
