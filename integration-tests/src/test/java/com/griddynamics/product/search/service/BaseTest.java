package com.griddynamics.product.search.service;

import com.griddynamics.product.indexer.repository.ProductRepositoryImpl;
import com.griddynamics.product.indexer.service.ProductServiceImpl;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
//import org.junit.runner.RunWith;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static io.restassured.RestAssured.given;


//@RunWith(SpringRunner.class)

//@RunWith(SpringJUnit4ClassRunner.class)

@ExtendWith(SpringExtension.class)
@ActiveProfiles("integration-test")
@SpringBootTest(
        classes = {
                ProductSearchApplication.class
        }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({ProductServiceImpl.class, ProductRepositoryImpl.class})
@TestPropertySource(locations = {"classpath:application-integration-test.yml"})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, MockitoTestExecutionListener.class})
public abstract class BaseTest {

    @LocalServerPort
    private int port;

    public int getSpringBootPort() {
        return port;
    }

    protected class APIClient {

        public RequestSpecification productSearchRequest() {
            return baseRequest()
                    .basePath("/v1/product")
                    .header("Content-Type", "application/json");
        }

        public RequestSpecification baseRequest() {
            RequestSpecification requestSpecification = given()
                    .baseUri("http://localhost").port(getSpringBootPort())
                    .log().all();


            return requestSpecification;
        }

    }
}
