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
class BrokerProductControlRestAssuredIT {

    private final static String ROOT_PATH = "/api/v1/brokers";

    @Autowired
    MockMvc mockMvc;

    Long brokerId;

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

    }

    @Test
    void addIndefiniteProductThenEnd() {
        Long productId =
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
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("fixedFeeAmt", equalTo(100))
                        .body("fromDate", equalTo(null))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "100",
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "toDate": "2020-10-10"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/product/{product-id}", brokerId, productId)
                .then()
                .status(HttpStatus.NO_CONTENT)
                .log();


    }

    @Test
    void addingOverlappingProductWithDifferentName() {
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
                .then()
                .status(HttpStatus.CREATED)
                .body("fixedFeeAmt", equalTo(100))
                .body("fromDate", equalTo(null))
                .log();

        with()
                .body("""
                        {
                            "name": "premium trading account",
                            "fixedFeeAmt": "100",
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("fixedFeeAmt", equalTo(100))
                .body("fromDate", equalTo(null))
                .log();

        with()
                .get(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.OK)
                .body("$.size()", equalTo(2))
                .body("name", hasItems("trading account", "premium trading account"))
                .log();
    }

    @Test
    void addingAnotherProductWithSameNameNotOverlapping() {
        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "100",
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("fixedFeeAmt", equalTo(100))
                .body("fromDate", equalTo(null))
                .log();

        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "105",
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "fromDate": "2020-10-11"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("fixedFeeAmt", equalTo(105))
                .body("fromDate", equalTo("2020-10-11"))
                .log();
    }

    @Test
    void addingAnotherProductWithSameNameOverlapping() {
        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "100",
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("fixedFeeAmt", equalTo(100))
                .body("fromDate", equalTo(null))
                .log();

        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "105",
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "fromDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-unique"))
                .body("detail", matchesPattern("^BrokerProduct: You provided the range 2020-10-10 -  but these periods overlap with it: \\[--2020-10-10 \\(id: [0-9]+\\)]$"))
                .log();
    }

    @Test
    void addingProductWithMissingDetails() {
        // missing name
        with()
                .body("""
                        {
                            "fixedFeeAmt": "100",
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("name"))
                .body("violations.message", hasItems("Every product must have a name."))
                .log();

        // missing from fee groups
        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "100",
                            "fixedFeePeriod": "MONTHLY",
                            "balanceFeePercent": "0.01",
                            "balanceFeePeriod": "MONTHLY",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("fixedFeeCurrency", "balanceFeeCurrency"))
                .body("violations.field", not(hasItem("balanceFeeMaxAmt")))
                .body("violations.message", hasItems("This field should be present for this fee type.", "Either none or all of the fee details should be present for this fee type."))
                .log();
    }

    @Test
    void addingWithNotParsableDetails() {
        // date
        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "100"
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "toDate": "2020-10-10QQQ"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("broker-entity/not-readable"))
                .body("detail", startsWith("JSON parse error"))
                .log();

        // feeperiod
        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "100"
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLYKKK",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("broker-entity/not-readable"))
                .body("detail", startsWith("JSON parse error"))
                .log();

        // bigdecimal
        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "100QQQ"
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("broker-entity/not-readable"))
                .body("detail", startsWith("JSON parse error"))
                .log();
    }

    @Test
    void updatingToHaveMissingDetails() {
        Long productId =
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
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("fixedFeeAmt", equalTo(100))
                        .body("fromDate", equalTo(null))
                        .log().body().extract().jsonPath().getLong("id");

        // missing name
        with()
                .body("""
                        {
                            "fixedFeeAmt": "100",
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "toDate": "2020-10-10"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/product/{product-id}", brokerId, productId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("name"))
                .body("violations.message", hasItems("Every product must have a name."))
                .log();

        // missing from fee groups
        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "100",
                            "fixedFeePeriod": "MONTHLY",
                            "balanceFeePercent": "0.01",
                            "balanceFeePeriod": "MONTHLY",
                            "toDate": "2020-10-10"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/product/{product-id}", brokerId, productId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("fixedFeeCurrency", "balanceFeeCurrency"))
                .body("violations.field", not(hasItem("balanceFeeMaxAmt")))
                .body("violations.message", hasItems("This field should be present for this fee type.", "Either none or all of the fee details should be present for this fee type."))
                .log();
    }

    @Test
    void updatingUnderAnotherBrokerId() {
        Long anotherBrokerId =
                        with()
                                .body("{\"name\":\"another broker\"}")
                                .post(ROOT_PATH)
                                .then()
                                .extract()
                                .body().jsonPath().getLong("id");
        Long productId =
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
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("fixedFeeAmt", equalTo(100))
                        .body("fromDate", equalTo(null))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "100",
                            "fixedFeeCurrency": "JPY",
                            "fixedFeePeriod": "MONTHLY"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/product/{product-id}", anotherBrokerId, productId)
                .then()
                .status(HttpStatus.NOT_FOUND)
                .body("type", equalTo("broker-entity/not-found"))
                .body("detail", matchesPattern("^No BrokerProduct with id: [0-9]+ linked to Broker with id: [0-9]+$"))
                .log();
    }

    @Test
    void updatingToOverlapSameName() {
        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "100",
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "toDate": "2021-10-09"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("fixedFeeAmt", equalTo(100))
                .body("fromDate", equalTo(null))
                .log();

        Long productId =
                with()
                        .body("""
                                {
                                    "name": "trading account",
                                    "fixedFeeAmt": "100",
                                    "fixedFeeCurrency": "HUF",
                                    "fixedFeePeriod": "MONTHLY",
                                    "fromDate": "2021-10-10"
                                }
                                """)
                        .post(ROOT_PATH + "/{id}/product", brokerId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("fixedFeeAmt", equalTo(100))
                        .body("fromDate", equalTo("2021-10-10"))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "100",
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "fromDate": "2021-10-09"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/product/{product-id}", brokerId, productId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-unique"))
                .body("detail", matchesPattern("^BrokerProduct: You provided the range 2021-10-09 -  but these periods overlap with it: \\[--2021-10-09 \\(id: [0-9]+\\)]$"))
                .log();
    }

    @Test
    void updatingToOverlapDifferentName() {
        with()
                .body("""
                        {
                            "name": "different name",
                            "fixedFeeAmt": "100",
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "toDate": "2021-10-09"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/product", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("fixedFeeAmt", equalTo(100))
                .body("fromDate", equalTo(null))
                .log();

        Long productId =
                with()
                        .body("""
                                {
                                    "name": "trading account",
                                    "fixedFeeAmt": "100",
                                    "fixedFeeCurrency": "HUF",
                                    "fixedFeePeriod": "MONTHLY",
                                    "fromDate": "2021-10-10"
                                }
                                """)
                        .post(ROOT_PATH + "/{id}/product", brokerId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("fixedFeeAmt", equalTo(100))
                        .body("fromDate", equalTo("2021-10-10"))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                            "name": "trading account",
                            "fixedFeeAmt": "100",
                            "fixedFeeCurrency": "HUF",
                            "fixedFeePeriod": "MONTHLY",
                            "fromDate": "2021-10-08"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/product/{product-id}", brokerId, productId)
                .then()
                .status(HttpStatus.NO_CONTENT)
                .log();
    }
}