package com.griddynamics.product.search.service.service;

import com.griddynamics.product.search.service.rest.ProductSearchRequest;
import com.griddynamics.product.search.service.rest.ProductSearchResponse;

public interface ProductSearchService {
    ProductSearchResponse search(ProductSearchRequest request);
}
