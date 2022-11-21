package app.broker;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.with;


@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"classpath:/cleanbroker.sql"})
class AccountProductAssociationControlRestAssuredIT {

    private final static String ROOT_PATH = "/api/v1/accounts";

    @Autowired
    MockMvc mockMvc;

    Long accountId;
    Long brokerId;
    Long productId2010s;
    Long productIdIndefinite;

    @BeforeEach
    void init() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        RestAssuredMockMvc.requestSpecification =
                given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON);

        accountId =
                with()
                        .body("""
                                {
                                    "name": "account",
                                    "accountType": "MAIN",
                                    "openedDate": "2020-01-01",
                                    "closedDate": "2022-01-01"
                                }
                                """)
                        .post(ROOT_PATH)
                        .then().extract().jsonPath().getLong("id");

        brokerId =
                with()
                        .body("{\"name\":\"diamond broker\"}")
                        .post("/api/v1/brokers")
                        .then()
                        .extract()
                        .body().jsonPath().getLong("id");

        productId2010s =
                with()
                        .body("""
                                {
                                    "name": "trading account",
                                    "fixedFeeAmt": "100",
                                    "fixedFeeCurrency": "HUF",
                                    "fixedFeePeriod": "MONTHLY",
                                    "fromDate": "2010-01-01",
                                    "toDate": "2020-12-31"
                                }
                                """)
                        .post("/api/v1/brokers" + "/{brokerId}/product", brokerId)
                        .then().extract().body().jsonPath().getLong("id");

        productIdIndefinite =
                with()
                        .body("""
                                {
                                    "name": "trading account",
                                    "fixedFeeAmt": "100",
                                    "fixedFeeCurrency": "HUF",
                                    "fixedFeePeriod": "MONTHLY"
                                }
                                """)
                        .post("/api/v1/brokers" + "/{id}/product", brokerId)
                        .then().extract().body().jsonPath().getLong("id");
    }


}
