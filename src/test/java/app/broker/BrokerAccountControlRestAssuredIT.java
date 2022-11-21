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
class BrokerAccountControlRestAssuredIT {

    private final static String ROOT_PATH = "/api/v1/accounts";

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void init() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        RestAssuredMockMvc.requestSpecification =
                given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON);

    }

    @Test
    void addingAccountWithNullOpenedDate() {
        with()
                .body("""
                        {
                            "name": "account",
                            "accountType": "MAIN"
                        }
                        """)
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("openedDate"))
                .body("violations.message", hasItems("Account open date must be specified."))
                .log();
    }

    @Test
    void addingAccountWithIndefiniteCloseDateThenChangingToEnd() {
        Long accountId =
                with()
                        .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2020-10-10"
                                }
                                """)
                        .post(ROOT_PATH)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("openedDate", equalTo("2020-10-10"))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                        {
                            "name": "account",
                            "accountType": "MAIN",
                            "openedDate": "2020-10-10",
                            "closedDate": "2020-11-11"
                        }
                        """)
                .put(ROOT_PATH + "/{id}", accountId)
                .then()
                .status(HttpStatus.NO_CONTENT)
                .log();
    }

    @Test
    void addingWithInvalidDetails() {
        with()
                .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2020-10-10",
                                    "closedDate": "2019-10-10"
                                }
                                """)
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("openedDate", "closedDate"))
                .body("violations.message", hasItems("Opened date must not be after closed date."))
                .log();
    }

    @Test
    void addingWithNotParsableDetails() {
        // account type
        with()
                .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAINQQQ",
                                    "openedDate": "2020-10-10"
                                }
                                """)
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("account-entity/not-readable"))
                .body("detail", startsWith("JSON parse error"))
                .log();

        // localdate
        with()
                .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2020-10-10QQQ"
                                }
                                """)
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("account-entity/not-readable"))
                .body("detail", startsWith("JSON parse error"))
                .log();
    }

    @Test
    void addingWithNonUniqueNameOverlapping() {
        with()
                .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2020-10-10"
                                }
                                """)
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.CREATED)
                .body("openedDate", equalTo("2020-10-10"))
                .log();
        with()
                .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2010-10-10",
                                    "closedDate": "2020-10-10"
                                }
                                """)
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-unique"))
                .body("detail", matchesPattern("The following accounts overlap with your request: \\[2020-10-10-- \\(id: [0-9]+\\)]"))
                .log();
    }

    @Test
    void addingWithNonUniqueNameNotOverlapping() {
        with()
                .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2020-10-10"
                                }
                                """)
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.CREATED)
                .body("openedDate", equalTo("2020-10-10"))
                .log();

        with()
                .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2010-10-10",
                                    "closedDate": "2020-10-08"
                                }
                                """)
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.CREATED)
                .body("closedDate", equalTo("2020-10-08"))
                .log();
    }

    @Test
    void updatingToInvalidDates() {
        Long accountId =
        with()
                .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2020-10-10"
                                }
                                """)
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.CREATED)
                .body("openedDate", equalTo("2020-10-10"))
                .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2020-10-10",
                                    "closedDate": "2019-01-01"
                                }
                                """)
                .put(ROOT_PATH + "/{id}", accountId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-valid"))
                .body("violations.field", hasItems("openedDate", "closedDate"))
                .body("violations.message", hasItems("Opened date must not be after closed date."))
                .log();
    }

    @Test
    void updatingToOverlapWithSameName() {
        // closed in 2019
        with()
                .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2010-10-10",
                                    "closedDate": "2019-10-10"
                                }
                                """)
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.CREATED)
                .body("closedDate", equalTo("2019-10-10"))
                .log();

        // opened in 2020
        Long accountId =
                with()
                        .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2020-10-10"
                                }
                                """)
                        .post(ROOT_PATH)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("openedDate", equalTo("2020-10-10"))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2019-10-10"
                                }
                                """)
                .put(ROOT_PATH + "/{id}", accountId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-unique"))
                .body("detail", matchesPattern("The following accounts overlap with your request: \\[2010-10-10--2019-10-10 \\(id: [0-9]+\\)]"))
                .log();
    }

    @Test
    void updatingToOverlapWithDifferentName() {
        with()
                .body("""
                                {
                                    "name": "different account",
                                    "accountType": "MAIN",
                                    "openedDate": "2010-01-01",
                                    "closedDate": "2019-10-10"
                                }
                                """)
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.CREATED)
                .body("closedDate", equalTo("2019-10-10"))
                .log();

        Long accountId =
                with()
                        .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2020-10-10"
                                }
                                """)
                        .post(ROOT_PATH)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("openedDate", equalTo("2020-10-10"))
                        .log().body().extract().jsonPath().getLong("id");

        with()
                .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2019-10-10"
                                }
                                """)
                .put(ROOT_PATH + "/{id}", accountId)
                .then()
                .status(HttpStatus.NO_CONTENT)
                .log();
    }
}
