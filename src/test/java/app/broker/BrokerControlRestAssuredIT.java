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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"classpath:/cleanbroker.sql"})
class BrokerControlRestAssuredIT {

    private final static String ROOT_PATH = "/api/v1/brokers";

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
    void renameBrokerValid() {

        Long brokerId =
                with()
                        .body("{\"name\":\"new broker\"}")
                        .post(ROOT_PATH)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("name", equalTo("new broker"))
                        .log().body()
                        .extract().body().jsonPath().getLong("id");

        with()
                .body("{\"name\":\"renamed broker\"}")
                .put(ROOT_PATH + "/{id}", brokerId)
                .then()
                .status(HttpStatus.NO_CONTENT)
                .log();
    }

    @Test
    void renameBrokerInvalidDuplicate() {

        with()
                .body("{\"name\":\"occupied name\"}")
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.CREATED)
                .body("name", equalTo("occupied name"))
                .log();

        Long brokerId =
                with()
                        .body("{\"name\":\"new broker\"}")
                        .post(ROOT_PATH)
                        .then()
                        .status(HttpStatus.CREATED)
                        .body("name", equalTo("new broker"))
                        .log().body()
                        .extract().body().jsonPath().getLong("id");

        with()
                .body("{\"name\":\"occupied name\"}")
                .put(ROOT_PATH + "/{id}", brokerId)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("type", equalTo("entity/not-unique"))
                .body("detail", equalTo("Cannot update broker. Another broker already exists with the name: occupied name"))
                .log();
    }

    @Test
    void listingAllBrokers() {
        with()
                .body("{\"name\":\"occupied name\"}")
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.CREATED)
                .body("name", equalTo("occupied name"))
                .log();

        with()
                .body("{\"name\":\"new broker\"}")
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.CREATED)
                .body("name", equalTo("new broker"))
                .log();

        with()
                .get(ROOT_PATH)
                .then()
                .status(HttpStatus.OK)
                .body("$.size()", equalTo(2))
                .body("name", hasItems("occupied name", "new broker"))
                .log();
    }
}
