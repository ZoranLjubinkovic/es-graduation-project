package com.griddynamics.product.search.service;

import com.griddynamics.product.indexer.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ExtendWith(SpringExtension.class)
public class ProductSearchIntegrationTests extends BaseTest {

    private APIClient client = new APIClient();

    @Autowired
    ProductService productService;

    static boolean created = false;

    @BeforeEach
    public void init() throws InterruptedException {
        if (!created) {
            productService.recreateIndex();
            created = true;
        }
        Thread.sleep(1000);
    }

    @Test
    public void testEmptyResponse1() {
        String queryText = "{}";
        client
                .productSearchRequest()
                .body(queryText)
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", equalTo(0));
    }

    @Test
    public void testEmptyResponse2() {
        String queryText = "Calvin klein L blue ankle skinny jeans wrongword";
        client
                .productSearchRequest()
                .body("{ \"queryText\" : \"" + queryText + "\"}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", equalTo(0));
    }

    @Test
    public void testEmptyResponse3() {
        String queryText = "Calvin klein L red ankle skinny jeans";
        client
                .productSearchRequest()
                .body("{ \"queryText\" : \"" + queryText + "\"}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", equalTo(0));
    }

    @Test
    public void testHappyPath() {
        String queryText = "Calvin klein L blue ankle skinny jeans";
        client
                .productSearchRequest()
                .body("{ \"queryText\" : \"" + queryText + "\"}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", equalTo(1))
                .body("products", hasSize(1))
                .body("products[0].id", is("2"))
                .body("products[0].brand", is("Calvin Klein"))
                .body("products[0].name", is("Women ankle skinny jeans, model 1282"))
                .body("products[0].skus", hasSize(9))
                .body("facets", notNullValue())
                .body("facets", hasSize(4))
                .body("facets[0][\"brand\"]", notNullValue())
                .body("facets[\"price\"]", notNullValue())
                .body("facets[2][\"color\"]", notNullValue())
                .body("facets[3][\"size\"]", notNullValue());

    }

    @Test
    public void testFacets() {
        String queryText = "jeans";
        client
                .productSearchRequest()
                .body("{ \"queryText\" : \"" + queryText + "\"}")
                .post()
                .then()
                .statusCode(200)
                .body("facets", notNullValue())
                .body("facets", hasSize(4))
                .body("facets[0][\"brand\"]", hasSize(2))
                .body("facets[0][\"brand\"][0].value", is("Calvin Klein"))
                .body("facets[0][\"brand\"][0].count", equalTo(4))
                .body("facets[0][\"brand\"][1].value", is("Levi's"))
                .body("facets[0][\"brand\"][1].count", equalTo(4))
                .body("facets[1][\"price\"]", hasSize(3))
                .body("facets[1][\"price\"][0].value", is("Cheap"))
                .body("facets[1][\"price\"][0].count", equalTo(2))
                .body("facets[1][\"price\"][1].value", is("Average"))
                .body("facets[1][\"price\"][1].count", equalTo(6))
                .body("facets[1][\"price\"][2].value", is("Expensive"))
                .body("facets[1][\"price\"][2].count", equalTo(0))
                .body("facets[2][\"color\"]", hasSize(4))
                .body("facets[2][\"color\"][0].value", is("Blue"))
                .body("facets[2][\"color\"][0].count", equalTo(8))
                .body("facets[2][\"color\"][1].value", is("Black"))
                .body("facets[2][\"color\"][1].count", equalTo(7))
                .body("facets[2][\"color\"][2].value", is("Red"))
                .body("facets[2][\"color\"][2].count", equalTo(1))
                .body("facets[2][\"color\"][3].value", is("White"))
                .body("facets[2][\"color\"][3].count", equalTo(1))
                .body("facets[3][\"size\"]", hasSize(6))
                .body("facets[3][\"size\"][0].value", is("L"))
                .body("facets[3][\"size\"][0].count", equalTo(8))
                .body("facets[3][\"size\"][1].value", is("M"))
                .body("facets[3][\"size\"][1].count", equalTo(8))
                .body("facets[3][\"size\"][2].value", is("S"))
                .body("facets[3][\"size\"][2].count", equalTo(6))
                .body("facets[3][\"size\"][3].value", is("XL"))
                .body("facets[3][\"size\"][3].count", equalTo(5))
                .body("facets[3][\"size\"][4].value", is("XXL"))
                .body("facets[3][\"size\"][4].count", equalTo(3))
                .body("facets[3][\"size\"][5].value", is("XS"))
                .body("facets[3][\"size\"][5].count", equalTo(2));

    }

    @Test
    public void testWomenAnkleBlueJeans() {
        String queryText = "women ankle blue jeans";
        client
                .productSearchRequest()
                .body("{ \"queryText\" : \"" + queryText + "\"}")
                .post()
                .then()
                .statusCode(200)
                .body("facets", notNullValue())
                .body("facets", hasSize(4))
                .body("facets[0][\"brand\"]", hasSize(2))
                .body("facets[0][\"brand\"][0].value", is("Calvin Klein"))
                .body("facets[0][\"brand\"][0].count", equalTo(2))
                .body("facets[0][\"brand\"][1].value", is("Levi's"))
                .body("facets[0][\"brand\"][1].count", equalTo(1))
                .body("facets[1][\"price\"]", hasSize(3))
                .body("facets[1][\"price\"][0].value", is("Cheap"))
                .body("facets[1][\"price\"][0].count", equalTo(0))
                .body("facets[1][\"price\"][1].value", is("Average"))
                .body("facets[1][\"price\"][1].count", equalTo(3))
                .body("facets[1][\"price\"][2].value", is("Expensive"))
                .body("facets[1][\"price\"][2].count", equalTo(0))
                .body("facets[2][\"color\"]", hasSize(4))
                .body("facets[2][\"color\"][0].value", is("Black"))
                .body("facets[2][\"color\"][0].count", equalTo(3))
                .body("facets[2][\"color\"][1].value", is("Blue"))
                .body("facets[2][\"color\"][1].count", equalTo(3))
                .body("facets[2][\"color\"][2].value", is("Red"))
                .body("facets[2][\"color\"][2].count", equalTo(1))
                .body("facets[2][\"color\"][3].value", is("White"))
                .body("facets[2][\"color\"][3].count", equalTo(1))
                .body("facets[3][\"size\"]", hasSize(4))
                .body("facets[3][\"size\"][0].value", is("L"))
                .body("facets[3][\"size\"][0].count", equalTo(3))
                .body("facets[3][\"size\"][1].value", is("M"))
                .body("facets[3][\"size\"][1].count", equalTo(3))
                .body("facets[3][\"size\"][2].value", is("S"))
                .body("facets[3][\"size\"][2].count", equalTo(3))
                .body("facets[3][\"size\"][3].value", is("XS"))
                .body("facets[3][\"size\"][3].count", equalTo(1));

    }

    @Test
    public void testSortAndBoost1() {
        String queryText = "jeans";
        client
                .productSearchRequest()
                .body("{ \"queryText\" : \"" + queryText + "\"}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", equalTo(8))
                .body("products[0].id", is("8"))
                .body("products[1].id", is("7"))
                .body("products[2].id", is("6"))
                .body("products[3].id", is("5"))
                .body("products[4].id", is("4"))
                .body("products[5].id", is("3"))
                .body("products[6].id", is("2"))
                .body("products[7].id", is("1"));
    }

    @Test
    public void testSortAndBoost2() {
        String queryText = "blue WOMEN jeans";
        client
                .productSearchRequest()
                .body("{ \"queryText\" : \"" + queryText + "\"}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", equalTo(5))
                .body("products[0].id", is("5"))
                .body("products[1].id", is("3"))
                .body("products[2].id", is("6"))
                .body("products[3].id", is("2"))
                .body("products[4].id", is("1"));
    }

    @Test
    public void testSortAndBoost3() {
        String queryText = "WOMEN blue jeans";
        client
                .productSearchRequest()
                .body("{ \"queryText\" : \"" + queryText + "\"}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", equalTo(5))
                .body("products[0].id", is("6"))
                .body("products[1].id", is("5"))
                .body("products[2].id", is("3"))
                .body("products[3].id", is("2"))
                .body("products[4].id", is("1"));
    }

    @Test
    public void testSortAndBoost4() {
        String queryText = "women ankle blue jeans";
        client
                .productSearchRequest()
                .body("{ \"queryText\" : \"" + queryText + "\"}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", equalTo(3))
                .body("products[0].id", is("6"))
                .body("products[1].id", is("2"))
                .body("products[2].id", is("1"));
    }

    @Test
    public void testPagination() {
        client
                .productSearchRequest()
                .body("{\n" +
                        "    \"queryText\" : \"jeans\",\n" +
                        "    \"size\" : 2,\n" +
                        "    \"page\" : 1\n" +
                        "}")
                .post()
                .then()
                .statusCode(200)
                .body("totalHits", equalTo(8))
                .body("products", hasSize(2))
                .body("products[0].id", is("6"))
                .body("products[1].id", is("5"));
    }
}
