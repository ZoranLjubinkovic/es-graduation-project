package com.griddynamics.product.search.service.service;

import com.griddynamics.product.search.service.repository.ProductSearchRepository;
import com.griddynamics.product.search.service.rest.ProductSearchRequest;
import com.griddynamics.product.search.service.rest.ProductSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    @Value("${com.griddynamics.product.search.service.request.default.findByQuerySize}")
    private Integer defaultQuerySize;
    @Value("${com.griddynamics.product.search.service.request.default.pageSize}")
    private Integer defaultPageSize;

    private ProductSearchRepository productRepository;

    public ProductSearchServiceImpl(ProductSearchRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductSearchResponse search(ProductSearchRequest request) {
        validateAndTransformRequest(request);
        if (request.getQueryText() == null || request.getQueryText().isEmpty()) {
            return ProductSearchResponse.builder()
                    .totalHits(0L)
                    .products(new ArrayList<>())
                    .build();
        }
        return productRepository.search(request);
    }

    private void validateAndTransformRequest(ProductSearchRequest request) {
        if (request.getSize() == null || request.getSize() <= 0) {
            request.setSize(defaultQuerySize);
        }
        if(request.getPage() == null || request.getPage() <= 0) {
            request.setPage(defaultPageSize);
        }
    }
}
