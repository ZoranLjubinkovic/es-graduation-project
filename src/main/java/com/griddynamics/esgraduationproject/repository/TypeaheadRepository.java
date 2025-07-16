package com.griddynamics.esgraduationproject.repository;

import com.griddynamics.esgraduationproject.model.TypeaheadServiceRequest;
import com.griddynamics.esgraduationproject.model.TypeaheadServiceResponse;

import java.time.format.DateTimeFormatter;

public interface TypeaheadRepository {
    TypeaheadServiceResponse getAllTypeaheads(TypeaheadServiceRequest request);
    TypeaheadServiceResponse getTypeaheadsByQuery(TypeaheadServiceRequest request);

    void recreateIndex();

    String yyyyMMddHHmmss = "yyyyMMddHHmmss";

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(yyyyMMddHHmmss);
}
