package com.griddynamics.product.search.service.rest;

import lombok.Data;

@Data
public class ProductSearchRequest {

    private String queryText;
    private Integer size;
    private Integer page;
}
