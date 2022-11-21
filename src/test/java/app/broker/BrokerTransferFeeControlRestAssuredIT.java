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
class BrokerTransferFeeControlRestAssuredIT {

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
    void addingIndefiniteFeeThenEnding() {
        Long feeId =
        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.23",
                            "feeCurrency": "EUR"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/transfer-fee", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("minimumFee", equalTo(3.23F))
                .body("toDate", equalTo(null))
                .body("brokerId", equalTo(brokerId.intValue()))
                .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.23",
                            "feeCurrency": "EUR",
                            "toDate": "2020-10-10"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/transfer-fee/{fee-id}", brokerId, feeId)
                .then()
                .status(HttpStatus.NO_CONTENT)
                .log();
    }

    @Test
    void addingAnotherFeeNotOverlappingThenListing() {
        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.23",
                            "feeCurrency": "EUR",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/transfer-fee", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("minimumFee", equalTo(3.23F))
                .body("toDate", equalTo("2020-10-10"))
                .body("brokerId", equalTo(brokerId.intValue()))
                .log();

        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.24",
                            "feeCurrency": "EUR",
                            "fromDate": "2020-10-11"
                        }
                        """)
                .post(ROOT_PATH+ "/{id}/transfer-fee", brokerId.intValue())
                .then()
                .status(HttpStatus.CREATED)
                .body("minimumFee", equalTo(3.24F))
                .body("fromDate", equalTo("2020-10-11"))
                .log();

        with()
                .get(ROOT_PATH + "/{id}/transfer-fee", brokerId)
                .then()
                .status(HttpStatus.OK)
                .body("$", hasSize(2))
                .body("minimumFee", hasItems(equalTo(3.23F), equalTo(3.24F)))
                .log();
    }

    @Test
    void addingAnotherFeeOverlapping() {
        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.23",
                            "feeCurrency": "EUR",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/transfer-fee", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("minimumFee", equalTo(3.23F))
                .body("toDate", equalTo("2020-10-10"))
                .body("brokerId", equalTo(brokerId.intValue()))
                .log();

        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.24",
                            "feeCurrency": "EUR",
                            "fromDate": "2019-10-11"
                        }
                        """)
                .post(ROOT_PATH+ "/{id}/transfer-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-unique"))
                .body("detail", matchesPattern("^BrokerTransferFee: You provided the range 2019-10-11 -  but these periods overlap with it: \\[--2020-10-10 \\(id: [0-9]+\\)]$"))
                .log();
    }

    @Test
    void addingWithMissingDetails() {
        // transferred currency missing and feecurrency missing
        with()
                .body("""
                        {
                            "minimumFee": "3.23",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/transfer-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("transferredCurrency", "feeCurrency"))
                .body("violations.message", hasItems("transferredCurrency must be present.", "feeCurrency must be present."))
                .log();

        // percent or min fee missing, max fee there
        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "maximumFee": "3.23",
                            "feeCurrency": "EUR",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/transfer-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("minimumFee", "percentFee"))
                .body("violations.message",hasItem("Either minimum fee or percent based fee must be present."))
                .log();
    }

    @Test
    void addingWithNotParsableDetails() {
        // date
        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.23",
                            "feeCurrency": "EUR",
                            "toDate": "2020-10-10QQ"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/transfer-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("broker-entity/not-readable"))
                .body("detail", startsWith("JSON parse error"))
                .log();

        // bigdecimal
        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.23",
                            "percentFee": "Q3",
                            "feeCurrency": "EUR",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/transfer-fee", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("broker-entity/not-readable"))
                .body("detail", startsWith("JSON parse error"))
                .log();
    }

    @Test
    void updatingFeeToHaveMissingDetails() {
        Long feeId =
                with()
                        .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.23",
                            "feeCurrency": "EUR"
                        }
                        """)
                        .post(ROOT_PATH + "/{id}/transfer-fee", brokerId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("minimumFee", equalTo(3.23F))
                        .body("toDate", equalTo(null))
                        .body("brokerId", equalTo(brokerId.intValue()))
                        .log().body().extract().jsonPath().getLong("id");

        // transferred currency missing and feecurrency missing
        with()
                .body("""
                        {
                            "minimumFee": "3.23",
                            "toDate": "2020-10-10"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/transfer-fee/{fee-id}", brokerId, feeId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("transferredCurrency", "feeCurrency"))
                .body("violations.message", hasItems("transferredCurrency must be present.", "feeCurrency must be present."))
                .log();

        // percent or min fee missing, max fee there
        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "maximumFee": "3.23",
                            "feeCurrency": "EUR",
                            "toDate": "2020-10-10"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/transfer-fee/{fee-id}", brokerId, feeId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("minimumFee", "percentFee"))
                .body("violations.message",hasItem("Either minimum fee or percent based fee must be present."))
                .log();
    }

    @Test
    void updatingToHaveOverlap() {
        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.23",
                            "feeCurrency": "EUR",
                            "toDate": "2020-10-10"
                        }
                        """)
                .post(ROOT_PATH + "/{id}/transfer-fee", brokerId)
                .then()
                .status(HttpStatus.CREATED)
                .body("minimumFee", equalTo(3.23F))
                .body("toDate", equalTo("2020-10-10"))
                .body("brokerId", equalTo(brokerId.intValue()))
                .log();

        Long feeId =
                with()
                        .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.23",
                            "feeCurrency": "EUR",
                            "fromDate": "2020-10-11"
                        }
                        """)
                        .post(ROOT_PATH + "/{id}/transfer-fee", brokerId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("minimumFee", equalTo(3.23F))
                        .body("toDate", equalTo(null))
                        .body("brokerId", equalTo(brokerId.intValue()))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.23",
                            "feeCurrency": "EUR",
                            "fromDate": "2019-10-11"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/transfer-fee/{fee-id}", brokerId, feeId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-unique"))
                .body("detail", matchesPattern("^BrokerTransferFee: You provided the range 2019-10-11 -  but these periods overlap with it: \\[--2020-10-10 \\(id: [0-9]+\\)]$"))
                .log();

    }

    @Test
    void updatingUnderAnotherBrokersId() {
        Long anotherBrokerId =
                        with()
                                .body("{\"name\":\"another broker\"}")
                                .post(ROOT_PATH)
                                .then()
                                .extract()
                                .body().jsonPath().getLong("id");
        Long feeId =
                with()
                        .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.23",
                            "feeCurrency": "EUR",
                            "fromDate": "2020-10-11"
                        }
                        """)
                        .post(ROOT_PATH + "/{id}/transfer-fee", brokerId)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("minimumFee", equalTo(3.23F))
                        .body("toDate", equalTo(null))
                        .body("brokerId", equalTo(brokerId.intValue()))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                            "transferredCurrency": "EUR",
                            "minimumFee": "3.23",
                            "feeCurrency": "EUR",
                            "fromDate": "2019-10-11"
                        }
                        """)
                .put(ROOT_PATH + "/{id}/transfer-fee/{fee-id}", anotherBrokerId, feeId)
                .then()
                .status(HttpStatus.NOT_FOUND)
                .body("type", equalTo("broker-entity/not-found"))
                .body("detail", matchesPattern("^No BrokerTransferFee with id: [0-9]+ linked to Broker with id: [0-9]+$"))
                .log();
    }
}