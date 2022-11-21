package app.manager;

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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;


@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"classpath:/cleanbroker.sql"})
class TransactionControllerRestAssuredIT {

    private final static String ROOT_PATH = "/api/v1/transactions";

    @Autowired
    MockMvc mockMvc;

    Long accountId;

    @BeforeEach
    void init() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        RestAssuredMockMvc.requestSpecification =
                given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON);

        accountId =
                with()
                        .body("{\"name\":\"account\", \"openedDate\": \"2010-01-01\"}")
                        .post("/api/v1/accounts")
                        .then()
                        .extract()
                        .body().jsonPath().getLong("id");
    }

    @Test
    void addingValidMoneyInTransaction() {
        with()
                .body("""
                        {
                            "date": "2020-01-01",
                            "transactionType": "MONEY_IN",
                            "addToAccountId": %d,
                            "assetAddedId": "EUR",
                            "countOfAssetAdded": 100
                        }
                        """.formatted(accountId))
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.CREATED)
                .body("$.size()", equalTo(1))
                .body("[0].countOfAssetAdded", equalTo(100))
                .body("[0].addToAccountId", equalTo(accountId.intValue()))
                .log();
    }

    // TODO: add product associations
    @Test
    void addingInvalidMoneyInTransaction() {
        // no currency
        with()
                .body("""
                        {
                            "date": "2020-01-01",
                            "transactionType": "MONEY_IN",
                            "addToAccountId": %d
                        }
                        """.formatted(accountId))
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("assetAddedId", "countOfAssetAdded"))
                .body("violations.message", hasItems("This field is required for this transaction type: MONEY_IN"))
                .log();
    }

}
