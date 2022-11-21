package app.broker;

import app.broker.fees.BrokerFeeService;
import app.broker.fees.global.BrokerGlobalFeeDto;
import app.broker.fees.global.CreateBrokerGlobalFeeCommand;
import app.broker.fees.global.UpdateBrokerGlobalFeeCommand;
import app.broker.fees.transfer.BrokerTransferFeeDto;
import app.broker.fees.transfer.CreateBrokerTransferFeeCommand;
import app.broker.fees.transfer.UpdateBrokerTransferFeeCommand;
import app.broker.product.BrokerProductDto;
import app.broker.product.BrokerProductService;
import app.broker.product.CreateBrokerProductCommand;
import app.broker.product.UpdateBrokerProductCommand;
import app.broker.product.commission.CreateCommissionCommand;
import app.broker.product.commission.ProductCommissionDto;
import app.broker.product.commission.UpdateCommissionCommand;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/${broker-controller.path}")
@AllArgsConstructor
@Slf4j
public class BrokerController {

    private final BrokerService brokerService;
    private final BrokerFeeService feeService;
    private final BrokerProductService productService;

    // BROKER
    @GetMapping
    public ResponseEntity<List<BrokerDto>> listBrokers() {
        return ResponseEntity.ok(brokerService.listBrokers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrokerDto> getBroker(@PathVariable("id") Long brokerId) {
        return ResponseEntity.ok(brokerService.getBrokerById(brokerId));
    }

    @PostMapping
    public ResponseEntity<BrokerDto> addBroker(@Valid @RequestBody CreateBrokerCommand command,
                                               @Value("${broker-controller.path}") String rootPath) {
        BrokerDto addedBroker = brokerService.addBroker(command);
        return ResponseEntity.created(URI.create(rootPath + "/" + addedBroker.getId()))
                .body(addedBroker);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBroker(@PathVariable("id") Long id, @Valid @RequestBody UpdateBrokerCommand command) {
        brokerService.updateBroker(id, command);
    }

    // GLOBAL FEE
    @GetMapping("/{brokerId}/global-fee")
    public ResponseEntity<List<BrokerGlobalFeeDto>> listGlobalFeeDetailsAtBroker(
            @PathVariable("brokerId") Long brokerId) {
        return ResponseEntity.ok(feeService.listGlobalFeesAtBroker(brokerId));
    }

    @GetMapping("/{brokerId}/global-fee/{feeId}")
    public ResponseEntity<BrokerGlobalFeeDto> getGlobalFee(@PathVariable("feeId") Long feeId) {
        return ResponseEntity.ok(feeService.getGlobalFeeById(feeId));
    }

    @PostMapping("/{brokerId}/global-fee")
    public ResponseEntity<BrokerGlobalFeeDto> addGlobalFeeDetails(
            @PathVariable("brokerId") Long brokerId, @Valid @RequestBody CreateBrokerGlobalFeeCommand command,
            @Value("${broker-controller.path}") String rootPath) {
        BrokerGlobalFeeDto globalFee = feeService.addGlobalFee(brokerId, command);
        return ResponseEntity.created(URI.create(rootPath + "/" + brokerId + "/global-fee/" + globalFee.getId()))
                .body(globalFee);
    }

    @PutMapping("/{brokerId}/global-fee/{feeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateGlobalFeeDetails(
            @PathVariable("brokerId") Long brokerId, @PathVariable("feeId") Long feeId,
            @Valid @RequestBody UpdateBrokerGlobalFeeCommand command) {
        feeService.updateGlobalFee(brokerId, feeId, command);
    }

    // TRANSFER FEE
    @GetMapping("/{brokerId}/transfer-fee")
    public ResponseEntity<List<BrokerTransferFeeDto>> listTransferFeeDetailsAtBroker(
            @PathVariable("brokerId") Long brokerId) {
        return ResponseEntity.ok(feeService.listTransferFeesAtBroker(brokerId));
    }

    @GetMapping("/{brokerId}/transfer-fee/{feeId}")
    public ResponseEntity<BrokerTransferFeeDto> getTransferFee(@PathVariable("feeId") Long feeId) {
        return ResponseEntity.ok(feeService.getTransferFeeById(feeId));
    }

    @PostMapping("/{brokerId}/transfer-fee")
    public ResponseEntity<BrokerTransferFeeDto> addTransferFeeDetails(
            @PathVariable("brokerId") Long brokerId, @Valid @RequestBody CreateBrokerTransferFeeCommand command,
            @Value("${broker-controller.path}") String rootPath) {
        BrokerTransferFeeDto transferFee = feeService.addTransferFee(brokerId, command);
        return ResponseEntity.created(URI.create(rootPath + "/" + brokerId + "/transfer-fee/" + transferFee.getId()))
                .body(transferFee);
    }

    @PutMapping("/{brokerId}/transfer-fee/{feeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateTransferFeeDetails(@PathVariable("brokerId") Long brokerId, @PathVariable("feeId") Long feeId,
                                         @Valid @RequestBody UpdateBrokerTransferFeeCommand command) {
        feeService.updateTransferFee(brokerId, feeId, command);
    }

    // BROKER PRODUCT
    @GetMapping("/{brokerId}/product")
    public ResponseEntity<List<BrokerProductDto>> listProductsAtBroker(@PathVariable("brokerId") Long brokerId) {
        return ResponseEntity.ok(productService.listProductsAtBroker(brokerId));
    }

    @PostMapping("/{brokerId}/product")
    public ResponseEntity<BrokerProductDto> addProductDetails(@PathVariable("brokerId") Long brokerId,
                                                              @Valid @RequestBody CreateBrokerProductCommand command,
                                                              @Value("${broker-controller.path}") String rootPath) {
        BrokerProductDto product = productService.addProduct(brokerId, command);
        return ResponseEntity.created(URI.create(rootPath + "/" + brokerId + "/product/" + product.getId()))
                .body(product);
    }

    @PutMapping("/{brokerId}/product/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProductDetails(@PathVariable("brokerId") Long brokerId, @PathVariable("productId") Long productId,
                                     @Valid @RequestBody UpdateBrokerProductCommand command) {
        productService.updateProduct(brokerId, productId, command);
    }

    // COMMISSION
    @GetMapping("/{brokerId}/product/{productId}/commission")
    public ResponseEntity<List<ProductCommissionDto>> listCommissionsForProduct(
            @PathVariable("brokerId") Long brokerId, @PathVariable("productId") Long productId) {
        return ResponseEntity.ok(productService.listCommissionsForProduct(productId));
    }

    @PostMapping("/{brokerId}/product/{productId}/commission")
    public ResponseEntity<ProductCommissionDto> addCommissionDetails(
            @PathVariable("brokerId") Long brokerId, @PathVariable("productId") Long productId,
            @Valid @RequestBody CreateCommissionCommand command, @Value("${broker-controller.path}") String rootPath) {
        ProductCommissionDto commission = productService.addCommission(productId, command);
        return ResponseEntity.created(URI.create(rootPath + "/" + brokerId + "/product/" + productId
                        + "/commission/" + commission.getId()))
                .body(commission);
    }

    @PutMapping("/{brokerId}/product/{productId}/commission/{commissionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateProductDetails(@PathVariable("brokerId") Long brokerId, @PathVariable("productId") Long productId,
                                     @PathVariable("commissionId") Long commissionId,
                                     @Valid @RequestBody UpdateCommissionCommand command) {
        productService.updateCommission(productId, commissionId, command);
    }

}
