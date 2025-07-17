package com.griddynamics.product.search.service.rest;

import com.griddynamics.product.search.service.service.ProductSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/product")
public class ProductSearchController {

    @Autowired
    private ProductSearchService productSearchService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ProductSearchResponse search(@RequestBody ProductSearchRequest request) {
        try {
            log.info("Received request {}", request);
            ProductSearchResponse productSearchResponse = productSearchService.search(request);
            log.info("productSearchResponse {}", productSearchResponse);
            return productSearchResponse;
        } catch (Throwable e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            log.error(e.getMessage(), e);
            return ProductSearchResponse
                    .builder()
                    .totalHits(0L)
                    .products(List.of())
                    .facets(List.of())
                    .build();
        }
    }
}
