package com.griddynamics.product.indexer.repository;

import java.time.format.DateTimeFormatter;

public interface ProductRepository {
    void recreateIndex();

    String yyyyMMddHHmmss = "yyyyMMddHHmmss";

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(yyyyMMddHHmmss);
}
