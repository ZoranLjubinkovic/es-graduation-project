package com.griddynamics.product.search.service.repository;

import com.griddynamics.product.search.service.rest.ProductSearchRequest;
import com.griddynamics.product.search.service.rest.ProductSearchResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ProductSearchRepository {

    String PRICE_RANGE_AGG = "priceRangeAgg";
    String BRAND_COUNT_AGG = "brandCountAgg";
    String SKUS_COLOR_NESTED_AGG = "skusColorAgg";
    String SKUS_SIZE_NESTED_AGG = "skusSizeAgg";
    String PRODUCT_COUNT = "product_count";

    String NAME_FIELD = "name";
    String BRAND_FIELD = "brand";
    String PRICE_FIELD = "price";
    String SKUS_SIZE_FIELD = "skus.size";
    String SKUS_COLOR_FIELD = "skus.color";
    String BRAND_SHINGLES = "brand.shingles";
    String NAME_SHINGLES = "name.shingles";
    String SCORE_FIELD = "_score";
    String ID_FIELD = "_id";


    Set<String> sizes = Set.of("xxs", "xs", "s", "m", "l", "xl", "xxl", "xxxl");
    Set<String> colors = Set.of("green", "black", "white", "blue", "yellow", "red", "brown", "orange", "grey");

    ProductSearchResponse search(ProductSearchRequest request);

    List<String> processTokens(Map<Integer, String> tokens);
}
