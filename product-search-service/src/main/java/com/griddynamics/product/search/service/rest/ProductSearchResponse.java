package com.griddynamics.product.search.service.rest;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductSearchResponse {

    private Long totalHits;
    private List<Map<String, Object>> products;
    private List<Map<String, Object>> facets;
}
