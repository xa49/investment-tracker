package app.broker.product;

import app.broker.BrokerDto;
import app.broker.BrokerService;
import app.broker.CreateBrokerCommand;
import app.broker.product.commission.CreateCommissionCommand;
import app.broker.product.commission.ProductCommissionDto;
import app.broker.product.commission.UpdateCommissionCommand;
import app.broker.BrokerEntityNotFoundException;
import app.broker.UniqueViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(scripts = "classpath:/cleanbroker.sql")
class BrokerProductServiceIT {

    @Autowired
    BrokerProductService productService;

    @Autowired
    BrokerService brokerService;

    Long brokerId;
    CreateBrokerProductCommand createProductCommand;
    UpdateBrokerProductCommand updateProductCommand;

    CreateCommissionCommand createCommissionCommand;

    UpdateCommissionCommand updateCommissionCommand;

    @BeforeEach
    void init() {
        brokerId = addABroker().getId();
        createProductCommand = new CreateBrokerProductCommand();
        updateProductCommand = new UpdateBrokerProductCommand();

        createCommissionCommand = new CreateCommissionCommand();
        updateCommissionCommand = new UpdateCommissionCommand();
    }

    @Test
    void addingAProduct() {
        createProductCommand.setName("Product");
        createProductCommand.setFixedFeeAmt(new BigDecimal("321.1"));
        BrokerProductDto dto = productService.addProduct(brokerId, createProductCommand);
        assertEquals(new BigDecimal("321.1"), dto.getFixedFeeAmt());
    }

    @Test
    void addingForInvalidBrokerShouldFail() {
        createProductCommand.setName("Product");
        BrokerEntityNotFoundException iae = assertThrows(BrokerEntityNotFoundException.class,
                () -> productService.addProduct(brokerId + 1, createProductCommand));
        assertThat(iae.getMessage(), matchesPattern("Broker not found with id: [0-9]+"));
    }

    @Test
    void addingWithSameNameToSameBrokerOverSamePeriodShouldFail() {
        createProductCommand.setName("Product");
        productService.addProduct(brokerId, createProductCommand);
        UniqueViolationException ex = assertThrows(UniqueViolationException.class,
                () -> productService.addProduct(brokerId, createProductCommand));
        assertThat(ex.getMessage(), startsWith("BrokerProduct: You provided the range  -  but these periods overlap with it: [-- (id: "));
    }

    @Test
    void addingWithSameNameToSameBrokerOverDifferentPeriodShouldSucceed() {
        createProductCommand.setName("Product");
        createProductCommand.setToDate(LocalDate.of(2000, 1, 1));
        productService.addProduct(brokerId, createProductCommand);

        createProductCommand.setFromDate(LocalDate.of(2010, 1, 1));
        createProductCommand.setToDate(null);
        BrokerProductDto dto = productService.addProduct(brokerId, createProductCommand);
        assertEquals(LocalDate.of(2010, 1, 1), dto.getFromDate());
        assertEquals("Product", dto.getName());
    }

    @Test
    void updatingProduct() {
        createProductCommand.setName("Product");
        BrokerProductDto dto = productService.addProduct(brokerId, createProductCommand);

        updateProductCommand.setName("Updated");
        BrokerProductDto updated = productService.updateProduct(brokerId, dto.getId(), updateProductCommand);
        assertEquals("Updated", updated.getName());
    }

    @Test
    void updatingProductDateToOverlappingShouldFail() {
        createProductCommand.setName("Product");
        createProductCommand.setFromDate(LocalDate.of(2000, 1, 1));
        productService.addProduct(brokerId, createProductCommand);

        // Same name
        createProductCommand.setFromDate(null);
        createProductCommand.setToDate(LocalDate.of(1990, 1, 1));
        BrokerProductDto dto = productService.addProduct(brokerId, createProductCommand);

        updateProductCommand.setName("Product");
        updateProductCommand.setToDate(LocalDate.of(2010, 1, 1));
        UniqueViolationException iae = assertThrows(UniqueViolationException.class,
                () -> productService.updateProduct(brokerId, dto.getId(), updateProductCommand));
        assertThat(iae.getMessage(), startsWith("BrokerProduct: You provided the range  - 2010-01-01 but these periods overlap with it: [2000-01-01-- (id: "));
    }

    @Test
    void updatingProductDateForDifferentNameButOverlappingTimeShouldSucceed() {
        createProductCommand.setName("Product 1");
        productService.addProduct(brokerId, createProductCommand);

        createProductCommand.setName("Product 2");
        BrokerProductDto dto = productService.addProduct(brokerId, createProductCommand);

        updateProductCommand.setToDate(LocalDate.of(2010, 1, 1));
        BrokerProductDto updated = productService.updateProduct(brokerId, dto.getId(), updateProductCommand);
        assertEquals(LocalDate.of(2010, 1, 1), updated.getToDate());
    }

    @Test
    void updatingProductIdForNotOwnerBrokerShouldFail() {
        BrokerProductDto dto = addProductNamedProductForTopLevelBroker();

        updateProductCommand.setName("Changed");
        BrokerEntityNotFoundException iae = assertThrows(BrokerEntityNotFoundException.class,
                () -> productService.updateProduct(brokerId + 1, dto.getId(), updateProductCommand));
        assertThat(iae.getMessage(), matchesPattern("Broker not found with id: [0-9]+"));
    }

    @Test
    void getProductByValidId() {
        BrokerProductDto dto = addProductNamedProductForTopLevelBroker();

        BrokerProductDto queried = productService.getProductById(dto.getId());
        assertEquals("Product", queried.getName());
    }

    @Test
    void getProductByInvalidId() {
        BrokerEntityNotFoundException iae = assertThrows(BrokerEntityNotFoundException.class,
                () -> productService.getProductById(0L));
        assertEquals("BrokerProduct not found with id: 0", iae.getMessage());
    }

    @Test
    void addCommissionsAndGet() {
        BrokerProductDto productDto = addProductNamedProductForTopLevelBroker();

        createCommissionCommand.setMarket("XETRA.FRANKFURT");
        createCommissionCommand.setMaximumFee(new BigDecimal("4"));
        ProductCommissionDto xetra = productService.addCommission(productDto.getId(), createCommissionCommand);
        assertEquals("XETRA.FRANKFURT", xetra.getMarket());
        assertEquals(new BigDecimal("4"), xetra.getMaximumFee());

        createCommissionCommand.setMarket("NYSE");
        ProductCommissionDto nyse = productService.addCommission(productDto.getId(), createCommissionCommand);
        assertEquals("NYSE", nyse.getMarket());

        ProductCommissionDto queried = productService.getCommissionById(nyse.getId());
        assertEquals("NYSE", queried.getMarket());
    }

    @Test
    void addingCommissionToNonExistingProductShouldFail() {
        createCommissionCommand.setMarket("Market");
        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> productService.addCommission(0L, createCommissionCommand));
        assertThat(ex.getMessage(), matchesPattern("BrokerProduct not found with id: [0-9]+"));
    }

    @Test
    void addingOverlappingCommissionForSameMarketAndProductShouldFail() {
        BrokerProductDto productDto = addProductNamedProductForTopLevelBroker();

        createCommissionCommand.setMarket("XETRA");
        createCommissionCommand.setFromDate(LocalDate.of(2000, 1, 1));
        productService.addCommission(productDto.getId(), createCommissionCommand);

        createCommissionCommand.setFromDate(LocalDate.of(1999, 1, 1));
        createCommissionCommand.setToDate(LocalDate.of(2010, 1, 10));
        UniqueViolationException ex = assertThrows(UniqueViolationException.class,
                () -> productService.addCommission(productDto.getId(), createCommissionCommand));
        assertThat(ex.getMessage(), startsWith("BrokerProductCommission: You provided the range 1999-01-01 - 2010-01-10 but these periods overlap with it: [2000-01-01-- (id: "));
    }

    @Test
    void addingNonOverlappingCommissionForSameMarketAndProductShouldSucceed() {
        BrokerProductDto productDto = addProductNamedProductForTopLevelBroker();

        createCommissionCommand.setMarket("XETRA");
        createCommissionCommand.setFromDate(LocalDate.of(2000, 1, 1));
        productService.addCommission(productDto.getId(), createCommissionCommand);

        createCommissionCommand.setFromDate(LocalDate.of(1999, 1, 1));
        createCommissionCommand.setToDate(LocalDate.of(1999, 6, 10));
        ProductCommissionDto dto = productService.addCommission(productDto.getId(), createCommissionCommand);
        assertEquals("XETRA", dto.getMarket());
        assertEquals(LocalDate.of(1999, 6, 10), dto.getToDate());

    }

    @Test
    void addingOverlappingCommissionForDifferentMarketAtSameBrokerShouldSucceed() {
        BrokerProductDto productDto = addProductNamedProductForTopLevelBroker();

        createCommissionCommand.setMarket("XETRA");
        createCommissionCommand.setFromDate(LocalDate.of(2000, 1, 1));
        productService.addCommission(productDto.getId(), createCommissionCommand);

        createCommissionCommand.setMarket("NYSE");
        createCommissionCommand.setFromDate(LocalDate.of(1999, 1, 1));
        createCommissionCommand.setToDate(LocalDate.of(2010, 6, 10));
        ProductCommissionDto dto = productService.addCommission(productDto.getId(), createCommissionCommand);
        assertEquals("NYSE", dto.getMarket());
        assertEquals(LocalDate.of(2010, 6, 10), dto.getToDate());
    }

    @Test
    void updatingCommissionSucceeds() {
        BrokerProductDto productDto = addProductNamedProductForTopLevelBroker();
        ProductCommissionDto commissionDto = addCommissionForXetra(productDto);

        updateCommissionCommand.setMarket("NYSE");
        ProductCommissionDto updated =
                productService.updateCommission(productDto.getId(), commissionDto.getId(), updateCommissionCommand);
        assertEquals("NYSE", updated.getMarket());

    }

    @Test
    void updatingNonExistingCommissionShouldFail() {
        BrokerProductDto productDto = addProductNamedProductForTopLevelBroker();
        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> productService.updateCommission(productDto.getId(), 0L, updateCommissionCommand));
        assertThat(ex.getMessage(), matchesPattern("No BrokerProductCommission with id: [0-9]+ linked to BrokerProduct with id: [0-9]+"));
    }

    @Test
    void updatingCommissionAtWrongProductShouldFail() {
        BrokerProductDto productDto = addProductNamedProductForTopLevelBroker();
        updateProductCommand.setFromDate(LocalDate.of(2000,1,1));
        updateProductCommand.setToDate(LocalDate.of(2001,1,1));
        productService.updateProduct(brokerId, productDto.getId(), updateProductCommand);

        ProductCommissionDto commissionDto = addCommissionForXetra(productDto);
        createProductCommand.setFromDate(LocalDate.of(2002,1,1));
        BrokerProductDto anotherProductDto = productService.addProduct(brokerId, createProductCommand);

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> productService.updateCommission(anotherProductDto.getId(), commissionDto.getId(), updateCommissionCommand));
        assertThat(ex.getMessage(), matchesPattern("No BrokerProductCommission with id: [0-9]+ linked to BrokerProduct with id: [0-9]+"));
    }

    @Test
    void changingCommissionToOverlappingAtSameMarketShouldFail() {
        BrokerProductDto productDto = addProductNamedProductForTopLevelBroker();
        ProductCommissionDto commissionDto = addCommissionForXetra(productDto);
        updateCommissionCommand.setMarket("XETRA");
        updateCommissionCommand.setFromDate(LocalDate.of(2000,1,1));
        updateCommissionCommand.setFromDate(LocalDate.of(2001,1,1));
        productService.updateCommission(productDto.getId(), commissionDto.getId(), updateCommissionCommand);

        createCommissionCommand.setMarket("XETRA");
        createCommissionCommand.setFromDate(LocalDate.of(1999,1,1));
        createCommissionCommand.setToDate(LocalDate.of(1999,6,1));
        ProductCommissionDto dto = productService.addCommission(productDto.getId(), createCommissionCommand);

        UniqueViolationException ex = assertThrows(UniqueViolationException.class,
                ( ) -> productService.updateCommission(productDto.getId(), dto.getId(), updateCommissionCommand));
        assertThat(ex.getMessage(), startsWith("BrokerProductCommission: You provided the range 2001-01-01 -  but these periods overlap with it: [2001-01-01-- (id: "));
    }

    @Test
    void changingCommissionToOverlappingAtAnotherMarketShouldSucceed() {
        BrokerProductDto productDto = addProductNamedProductForTopLevelBroker();

        // XETRA 2000-01-01 to 2001-01-01
        ProductCommissionDto commissionDto = addCommissionForXetra(productDto);
        updateCommissionCommand.setMarket("XETRA");
        updateCommissionCommand.setFromDate(LocalDate.of(2000,1,1));
        updateCommissionCommand.setToDate(LocalDate.of(2001,1,1));
        productService.updateCommission(productDto.getId(), commissionDto.getId(), updateCommissionCommand);

        createCommissionCommand.setMarket("XETRA");
        createCommissionCommand.setFromDate(LocalDate.of(1999,1,1));
        createCommissionCommand.setToDate(LocalDate.of(1999,6,1));
        ProductCommissionDto dto = productService.addCommission(productDto.getId(), createCommissionCommand);

        updateCommissionCommand.setMarket("NYSE");
        ProductCommissionDto updated = productService.updateCommission(productDto.getId(), dto.getId(), updateCommissionCommand);
        assertEquals(LocalDate.of(2000,1,1), updated.getFromDate());
    }

    @Test
    void gettingCommissionForExistingProductMarketDate() {
        BrokerProductDto productDto = addProductNamedProductForTopLevelBroker();
        ProductCommissionDto xetra = addCommissionForXetra(productDto);
        updateCommissionCommand.setFromDate(LocalDate.of(2000,1,1));
        updateCommissionCommand.setToDate(LocalDate.of(2001,1,1));
        updateCommissionCommand.setMarket("XETRA");
        productService.updateCommission(productDto.getId(), xetra.getId(), updateCommissionCommand);

        ProductCommissionDto queried = productService.getCommission(productDto.getId(), "XETRA",
                LocalDate.of(2000,6,7));
        assertEquals("XETRA", queried.getMarket());
        assertEquals(LocalDate.of(2001,1,1), queried.getToDate());
    }

    @Test
    void gettingCommissionForNonExistingMarket() {
        BrokerProductDto productDto = addProductNamedProductForTopLevelBroker();

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> productService.getCommission(productDto.getId(), "NYSE", LocalDate.of(2000,1,1)));
        assertThat(ex.getMessage(), matchesPattern("ProductCommission not found for product: [0-9]+ and market: NYSE date: 2000-01-01"));
    }

    @Test
    void gettingCommissionForOutOfBoundsDate() {
        BrokerProductDto productDto = addProductNamedProductForTopLevelBroker();
        ProductCommissionDto xetra = addCommissionForXetra(productDto);
        updateCommissionCommand.setFromDate(LocalDate.of(2000,1,1));
        updateCommissionCommand.setToDate(LocalDate.of(2001,1,1));
        productService.updateCommission(productDto.getId(), xetra.getId(), updateCommissionCommand);

        BrokerEntityNotFoundException ex = assertThrows(BrokerEntityNotFoundException.class,
                () -> productService.getCommission(productDto.getId(), "XETRA",
                LocalDate.of(2002,6,7)));
        assertThat(ex.getMessage(), matchesPattern("ProductCommission not found for product: [0-9]+ and market: XETRA date: 2002-06-07"));
    }


    BrokerProductDto addProductNamedProductForTopLevelBroker() {
        createProductCommand.setName("Product");
        return productService.addProduct(brokerId, createProductCommand);
    }


    BrokerDto addABroker() {
        CreateBrokerCommand command = new CreateBrokerCommand();
        command.setName("Broker");
        return brokerService.addBroker(command);
    }

    private ProductCommissionDto addCommissionForXetra(BrokerProductDto productDto) {
        createCommissionCommand.setMarket("XETRA");
        return productService.addCommission(productDto.getId(), createCommissionCommand);
    }

}