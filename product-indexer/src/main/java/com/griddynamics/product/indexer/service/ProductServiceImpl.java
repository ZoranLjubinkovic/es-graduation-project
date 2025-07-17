package com.griddynamics.product.indexer.service;

import com.griddynamics.product.indexer.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void recreateIndex() {
        productRepository.recreateIndex();
    }
}
