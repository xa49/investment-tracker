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

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.with;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"classpath:/cleanbroker.sql"})
class BrokerGlobalFeeControlRestAssuredIT {

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
    void addingThenModifyingIndefinitePeriodGlobalFee() {
        Long feeId =
                with()
                        .body("""
                                {
                                  "fixedFeeGlobalLimit": "230.56",
                                  "fixedFeeGlobalLimitCurrency": "HUF",
                                  "fixedFeeGlobalLimitPeriod": "MONTHLY",
                                  "referencePaymentDate": "2020-10-10"
                                }
                                """)
                        .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("fixedFeeGlobalLimit", equalTo(230.56F))
                        .body("fromDate", equalTo(null))
                        .body("balanceFeeGlobalLimitPeriod", equalTo(null))
                        .log().body()
                        .extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "230.56",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "MONTHLY",
                          "referencePaymentDate": "2020-10-10",
                          "toDate": "2020-12-31"
                        }
                        """
                )
                .put(ROOT_PATH + "/{id}/global-fee/{fee-id}", brokerId, feeId)
                .then()
                .status(HttpStatus.NO_CONTENT)
                .log();
    }

    @Test
    void addingAnotherFeeNotOverlappingThenListing() {
        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "230.56",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "MONTHLY",
                          "referencePaymentDate": "2020-10-10",
                          "toDate": "2020-12-31"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("fixedFeeGlobalLimit", equalTo(230.56F))
                .body("fromDate", equalTo(null))
                .body("balanceFeeGlobalLimitPeriod", equalTo(null))
                .log();

        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "330.3333",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "MONTHLY",
                          "referencePaymentDate": "2022-10-08",
                          "fromDate": "2021-01-01"
                        }
                        """
                )
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("fixedFeeGlobalLimit", equalTo(330.3333F))
                .body("toDate", equalTo(null))
                .log();

        with()
                .get(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.OK)
                .body("$.size()", equalTo(2))
                .body("fixedFeeGlobalLimit", hasSize(2))
                .body("fixedFeeGlobalLimit", hasItems(equalTo(230.56F), equalTo(330.3333F)))
                .log();

    }

    @Test
    void addingAnotherFeeOverlapping() {
        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "230.56",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "MONTHLY",
                          "referencePaymentDate": "2020-10-10",
                          "toDate": "2020-12-31"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("fixedFeeGlobalLimit", equalTo(230.56F))
                .body("fromDate", equalTo(null))
                .body("balanceFeeGlobalLimitPeriod", equalTo(null))
                .log();

        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "330.3333",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "MONTHLY",
                          "referencePaymentDate": "2022-10-08",
                          "fromDate": "2020-01-01"
                        }
                        """
                )
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-unique"))
                .body("detail", matchesPattern("^BrokerGlobalFee: You provided the range 2020-01-01 -  but these periods overlap with it: \\[--2020-12-31 \\(id: [0-9]+\\)]$"))
                .log();
    }

    @Test
    void addingFeesWithMissingDetails() {
        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "230.56",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "referencePaymentDate": "2020-10-10",
                          "toDate": "2020-12-31"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("fixedFeeGlobalLimitPeriod"))
                .body("violations.message", hasItems("Either none or all of the fee details should be present for this fee type."))
                .log();

        with()
                .body("""
                        {
                          "globalFixedFeeAmt": "230.56",
                          "referencePaymentDate": "2020-10-10",
                          "toDate": "2020-12-31"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("globalFixedFeeCurrency", "globalFixedFeePeriod"))
                .body("violations.message", hasItem("Either none or all of the fee details should be present for this fee type."))
                .log();

        with()
                .body("""
                        {
                          "balanceFeeGlobalLimitCurrency": "HUF",
                          "balanceFeeGlobalLimitPeriod": "MONTHLY",
                          "referencePaymentDate": "2020-10-10",
                          "toDate": "2020-12-31"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("balanceFeeGlobalLimit"))
                .body("violations.message", hasItems("Either none or all of the fee details should be present for this fee type."))
                .log();

        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "230.56",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "MONTHLY",
                          "toDate": "2020-12-31"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("referencePaymentDate"))
                .body("violations.message", hasItems("If any fee is present, a reference payment date must be provided."))
                .log();
    }

    @Test
    void addingFeesWithUnparsableDetails() {
        // FeePeriod
        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "230.56",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "WQHWQHWHQWHWQ",
                          "referencePaymentDate": "2020-10-10",
                          "toDate": "2020-12-31"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("broker-entity/not-readable"))
                .body("detail", startsWith("JSON parse error: Cannot deserialize value of "))
                .log();

        // LocalDate
        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "230.56",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "MONTHLY",
                          "referencePaymentDate": "Q2020-10-10",
                          "toDate": "2020-12-31"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("broker-entity/not-readable"))
                .body("detail", startsWith("JSON parse error: Cannot deserialize value of type"))
                .log();

        // BigDecimal
        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "230.56TTT",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "MONTHLY",
                          "referencePaymentDate": "2020-10-10",
                          "toDate": "2020-12-31"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("broker-entity/not-readable"))
                .body("detail", startsWith("JSON parse error: Cannot deserialize value of type"))
                .log();
    }

    @Test
    void addingValidityDatesNotValidFee() {
        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "230.56",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "MONTHLY",
                          "referencePaymentDate": "2020-10-10",
                          "fromDate": "2021-01-01",
                          "toDate": "2020-12-31"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("fromDate", "toDate"))
                .body("violations.message", hasItem("Valid-from date must not be after valid-to date."))
                .log();
    }

    @Test
    void updatingFeeToHaveMissingDetails() {
        Long feeId =
                with()
                        .body("""
                                {
                                  "fixedFeeGlobalLimit": "230.56",
                                  "fixedFeeGlobalLimitCurrency": "HUF",
                                  "fixedFeeGlobalLimitPeriod": "MONTHLY",
                                  "referencePaymentDate": "2020-10-10"
                                }
                                """)
                        .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("fixedFeeGlobalLimit", equalTo(230.56F))
                        .body("fromDate", equalTo(null))
                        .body("balanceFeeGlobalLimitPeriod", equalTo(null))
                        .log().body()
                        .extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "230.56",
                          "toDate": "2020-10-10"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/global-fee/{fee-id}", brokerId, feeId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("fixedFeeGlobalLimitPeriod", "fixedFeeGlobalLimitCurrency", "referencePaymentDate"))
                .body("violations.message", hasItems("Either none or all of the fee details should be present for this fee type.", "If any fee is present, a reference payment date must be provided."))
                .log();
    }

    @Test
    void updatingFeeToOverlap() {
        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "230.56",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "MONTHLY",
                          "referencePaymentDate": "2020-10-10",
                          "toDate": "2020-12-31"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("fixedFeeGlobalLimit", equalTo(230.56F))
                .body("fromDate", equalTo(null))
                .body("balanceFeeGlobalLimitPeriod", equalTo(null))
                .log();

        Long feeId =
                with()
                        .body("""
                                {
                                  "fixedFeeGlobalLimit": "330.3333",
                                  "fixedFeeGlobalLimitCurrency": "HUF",
                                  "fixedFeeGlobalLimitPeriod": "MONTHLY",
                                  "referencePaymentDate": "2022-10-08",
                                  "fromDate": "2021-01-01"
                                }
                                """
                        )
                        .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("fixedFeeGlobalLimit", equalTo(330.3333F))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "330.3333",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "MONTHLY",
                          "referencePaymentDate": "2022-10-08",
                          "fromDate": "2020-01-01"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/global-fee/{feeId}", brokerId, feeId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-unique"))
                .body("detail", matchesPattern("^BrokerGlobalFee: You provided the range 2020-01-01 -  but these periods overlap with it: \\[--2020-12-31 \\(id: [0-9]+\\)]$"))
                .log();

    }

    @Test
    void updatingToNotValidDates() {
        Long feeId =
                with()
                        .body("""
                                {
                                  "fixedFeeGlobalLimit": "330.3333",
                                  "fixedFeeGlobalLimitCurrency": "HUF",
                                  "fixedFeeGlobalLimitPeriod": "MONTHLY",
                                  "referencePaymentDate": "2022-10-08",
                                  "fromDate": "2021-01-01"
                                }
                                """
                        )
                        .post(ROOT_PATH + "/{id}/global-fee", brokerId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("fixedFeeGlobalLimit", equalTo(330.3333F))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                          "fixedFeeGlobalLimit": "330.3333",
                          "fixedFeeGlobalLimitCurrency": "HUF",
                          "fixedFeeGlobalLimitPeriod": "MONTHLY",
                          "referencePaymentDate": "2022-10-08",
                          "fromDate": "2021-01-01",
                          "toDate": "2020-12-21"
                        }
                        """
                )
                .put(ROOT_PATH + "/{id}/global-fee/{feeId}", brokerId, feeId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("fromDate", "toDate"))
                .body("violations.message", hasItem("Valid-from date must not be after valid-to date."))
                .log();
    }

}
