package com.griddynamics.product.search.service.repository;

//import org.junit.Test;
//import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import static org.junit.;
//import static org.junit.Assert.assertTrue;

public class ProductSearchRepositoryImplTest {

    ProductSearchRepository repository = new ProductSearchRepositoryImpl(Mockito.any());

    @Test
    public void testEmptyMap() {
        Map<Integer, String> tokens = new HashMap<>();
        List<String> result = repository.processTokens(tokens);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testNullMap() {
        List<String> result = repository.processTokens(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSingleToken() {
        Map<Integer, String> tokens = new HashMap<>();
        tokens.put(0, "women");

        List<String> result = repository.processTokens(tokens);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testPhrases() {

        Map<Integer, String> tokens =  new HashMap<>();
        tokens.put(1, "women");
        tokens.put(2, "jeans");
        tokens.put(4, "calvin");
        tokens.put(5, "klein");
        List<String> result = repository.processTokens(tokens);

        assertEquals(2, result.size());
        assertEquals("women jeans", result.get(0));
        assertEquals("calvin klein", result.get(1));
    }

    @Test
    public void testLongPhrase() {
        Map<Integer, String> tokens = new HashMap<>();
        tokens.put(0, "women");
        tokens.put(1, "high");
        tokens.put(2, "waisted");
        tokens.put(5, "ankle");
        tokens.put(6, "skinny");
        tokens.put(8, "jeans");

        List<String> result = repository.processTokens(tokens);

        assertEquals(2, result.size());
        assertEquals("women high waisted", result.get(0));
        assertEquals("ankle skinny", result.get(1));
    }

    @Test
    public void testLongPhrase2() {
        Map<Integer, String> tokens = new HashMap<>();
        tokens.put(0, "women");
        tokens.put(1, "high");
        tokens.put(2, "waisted");
        tokens.put(3, "ankle");
        tokens.put(4, "skinny");
        tokens.put(5, "jeans");

        List<String> result = repository.processTokens(tokens);

        assertEquals(1, result.size());
        assertEquals("women high waisted ankle skinny jeans", result.get(0));
    }

}