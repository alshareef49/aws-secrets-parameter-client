package com.aws.config.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

@Slf4j
public class ParameterStore {
    public static JsonNode getParameterStoreValue(String parameter) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        SsmClient client = SsmClient.builder().build();
        GetParameterRequest parameterRequest = GetParameterRequest.builder().name(parameter).build();
        GetParameterResponse parameterResponse = client.getParameter(parameterRequest);
        String parameterValue = parameterResponse.parameter().value();
        JsonNode jsonNode = mapper.readTree(parameterValue);
        log.info("Retrieved ParameterStore values:{}",parameterValue);
        return jsonNode;
    }
}
