package app.broker;

import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.with;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest
@AutoConfigureMockMvc
class BrokerServiceRestAssuredIT {

    private final static String ROOT_PATH  = "/api/v1/brokers";

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
    void addingDuplicateNamedBrokersShouldFail() {
        with()
                .body(new CreateBrokerCommand("broker 1"))
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.CREATED)
                .body("name", equalTo("broker 1"))
                .log();

        with()
                .body(new CreateBrokerCommand("broker 1"))
                .post(ROOT_PATH)
                .then()
                .status(HttpStatus.BAD_REQUEST)
                .body("detail", equalTo("Broker names must be unique. A broker already exists with the name: broker 1"))
                .log();
    }
}
