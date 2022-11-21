package app.broker;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"classpath:/cleanbroker.sql"})
class BrokerProductCommissionControlRestAssuredIT {

    private final static String ROOT_PATH = "/api/v1/brokers";

    @Autowired
    MockMvc mockMvc;

    Long brokerId;
    Long productId;

    @BeforeEach
    void init() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        RestAssuredMockMvc.requestSpecification =
                given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON);

        brokerId =
                with()
                        .body("{\"name\":\"diamond broker\"}")
                        .post(ROOT_PATH)
                        .then()
                        .extract()
                        .body().jsonPath().getLong("id");

        productId =
                with()
                        .body("""
                                {
                                    "name": "trading account",
                                    "fixedFeeAmt": "100",
                                    "fixedFeeCurrency": "HUF",
                                    "fixedFeePeriod": "MONTHLY"
                                }
                                """)
                        .post(ROOT_PATH + "/{id}/product", brokerId)
                        .then().extract().body().jsonPath().getLong("id");
    }

    @Test
    void addingIndefiniteCommissionThenChangingToEnd() {
        Long commissionId =
                with()
                        .body("""
                                {
                                    "market": "Amsterdam",
                                    "percentFee": "0.25",
                                    "minimumFee": "7",
                                    "currency": "EUR"
                                }
                                """)
                        .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("percentFee", equalTo(0.25F))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                            "market": "Amsterdam",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR",
                            "toDate": "2020-10-10"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/product/{product-id}/commission/{commission-id}", brokerId, productId, commissionId)
                .then()
                .status(HttpStatus.NO_CONTENT)
                .log();

    }

    @Test
    void addingOverlappingCommissionAtDifferentMarketThenListing() {
        with()
                .body("""
                        {
                            "market": "Amsterdam",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                .then()
                .status(HttpStatus.CREATED)
                .body("percentFee", equalTo(0.25F))
                .log();

        with()
                .body("""
                        {
                            "market": "Frankfurt",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                .then()
                .status(HttpStatus.CREATED)
                .body("percentFee", equalTo(0.25F))
                .log();

        with()
                .get(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                .then()
                .status(HttpStatus.OK)
                .body("$.size()", equalTo(2))
                .body("market", hasItems("Amsterdam", "Frankfurt"))
                .log();
    }

    @Test
    void addingCommissionSameMarketNotOverlapping() {
        with()
                .body("""
                        {
                            "market": "Amsterdam",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                .then()
                .status(HttpStatus.CREATED)
                .body("percentFee", equalTo(0.25F))
                .log();

        with()
                .body("""
                        {
                            "market": "Amsterdam",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR",
                            "fromDate": "2020-10-11"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                .then()
                .status(HttpStatus.CREATED)
                .body("percentFee", equalTo(0.25F))
                .body("fromDate", equalTo("2020-10-11"))
                .log();
    }

    @Test
    void addingCommissionSameMarketOverlapping() {
        with()
                .body("""
                        {
                            "market": "Amsterdam",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                .then()
                .status(HttpStatus.CREATED)
                .body("percentFee", equalTo(0.25F))
                .log();

        with()
                .body("""
                        {
                            "market": "Amsterdam",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR",
                            "fromDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-unique"))
                .body("detail", matchesPattern("^BrokerProductCommission: You provided the range 2020-10-10 -  but these periods overlap with it: \\[--2020-10-10 \\(id: [0-9]+\\)]$"))
                .log();
    }

    @Test
    void addingCommissionWithMissingDetails() {
        // market and one of percent and min fee missing
        with()
                .body("""
                        {
                            "maximumFee": "7",
                            "currency": "EUR",
                            "fromDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("market", "percentFee", "minimumFee"))
                .body("violations.message", hasItems("Market must be present.","Either percent fee or minimum fee must be present."))
                .log();
    }

    @Test
    void addingCommissionWithNotParsableDetails() {
        // localdate
        with()
                .body("""
                        {
                            "market": "Amsterdam",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR",
                            "toDate": "2020-10-10QQQ"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("broker-entity/not-readable"))
                .body("detail", startsWith("JSON parse error"))
                .log();

        // bigdecimal
        with()
                .body("""
                        {
                            "market": "Amsterdam",
                            "percentFee": "0.25RRRR",
                            "minimumFee": "7",
                            "currency": "EUR",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("broker-entity/not-readable"))
                .body("detail", startsWith("JSON parse error"))
                .log();
    }

    @Test
    void updatingCommissionToHaveMissingDetails() {
        Long commissionId =
                with()
                        .body("""
                                {
                                    "market": "Amsterdam",
                                    "percentFee": "0.25",
                                    "minimumFee": "7",
                                    "currency": "EUR",
                                    "toDate": "2020-10-10"
                                }
                                """)
                        .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("percentFee", equalTo(0.25F))
                        .log().body().extract().jsonPath().getLong("id");

        // market and one of percent and min fee missing
        with()
                .body("""
                        {
                            "maximumFee": "7",
                            "currency": "EUR",
                            "fromDate": "2020-10-10"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/product/{product-id}/commission/{commission-id}", brokerId, productId, commissionId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("market", "percentFee", "minimumFee"))
                .body("violations.message", hasItems("Market must be present.","Either percent fee or minimum fee must be present."))
                .log();
    }

    @Test
    void updatingCommissionToOverlapSameMarket() {
        with()
                .body("""
                        {
                            "market": "Amsterdam",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR",
                            "fromDate": "2020-10-11"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                .then()
                .status(HttpStatus.CREATED)
                .body("percentFee", equalTo(0.25F))
                .log();

        Long commissionId =
                with()
                        .body("""
                                {
                                    "market": "Amsterdam",
                                    "percentFee": "0.25",
                                    "minimumFee": "7",
                                    "currency": "EUR",
                                    "toDate": "2020-10-10"
                                }
                                """)
                        .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("percentFee", equalTo(0.25F))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                            "market": "Amsterdam",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR",
                            "toDate": "2020-10-11"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/product/{product-id}/commission/{commission-id}", brokerId, productId, commissionId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-unique"))
                .body("detail", matchesPattern("^BrokerProductCommission: You provided the range  - 2020-10-11 but these periods overlap with it: \\[2020-10-11-- \\(id: [0-9]+\\)]$"))
                .log();

    }

    @Test
    void updatingCommissionToOverlapDifferentMarket() {
        with()
                .body("""
                        {
                            "market": "Frankfurt",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR",
                            "fromDate": "2020-10-11"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                .then()
                .status(HttpStatus.CREATED)
                .body("percentFee", equalTo(0.25F))
                .log();

        Long commissionId =
                with()
                        .body("""
                                {
                                    "market": "Amsterdam",
                                    "percentFee": "0.25",
                                    "minimumFee": "7",
                                    "currency": "EUR",
                                    "toDate": "2020-10-10"
                                }
                                """)
                        .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("percentFee", equalTo(0.25F))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                            "market": "Amsterdam",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR",
                            "toDate": "2020-10-14"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/product/{product-id}/commission/{commission-id}", brokerId, productId, commissionId)
                .then()
                .status(HttpStatus.NO_CONTENT)
                .log();
    }

    @Test
    void updatingCommissionAtAnotherProduct() {
        Long anotherProductId =
                with()
                        .body("""
                                {
                                    "name": "trading account premium",
                                    "fixedFeeAmt": "100",
                                    "fixedFeeCurrency": "HUF",
                                    "fixedFeePeriod": "MONTHLY"
                                }
                                """)
                        .post(ROOT_PATH + "/{id}/product", brokerId)
                        .then().extract().body().jsonPath().getLong("id");

        Long commissionId =
                with()
                        .body("""
                                {
                                    "market": "Amsterdam",
                                    "percentFee": "0.25",
                                    "minimumFee": "7",
                                    "currency": "EUR",
                                    "toDate": "2020-10-10"
                                }
                                """)
                        .post(ROOT_PATH + "/{id}/product/{product-id}/commission", brokerId, productId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("percentFee", equalTo(0.25F))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                            "market": "Amsterdam",
                            "percentFee": "0.25",
                            "minimumFee": "7",
                            "currency": "EUR",
                            "toDate": "2020-10-14"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/product/{product-id}/commission/{commission-id}", brokerId, anotherProductId, commissionId)
                .then()
                .status(HttpStatus.NOT_FOUND)
                .body("type", equalTo("broker-entity/not-found"))
                .body("detail", matchesPattern("No BrokerProductCommission with id: [0-9]+ linked to BrokerProduct with id: [0-9]+"))
                .log();
    }

}
