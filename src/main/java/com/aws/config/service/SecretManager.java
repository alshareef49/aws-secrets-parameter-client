package com.aws.config.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@Slf4j
public class SecretManager {
    private SecretsManagerClient client;

    public SecretManager(SecretsManagerClient secretsManagerClient){
        this.client = secretsManagerClient;
    }

    public static JsonNode getSecretValues(SecretsManagerClient client,String secretName) throws JsonProcessingException{
        ObjectMapper mapper = new ObjectMapper();
        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder().secretId(secretName).build();
        GetSecretValueResponse getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
        String secret = getSecretValueResponse.secretString();
        JsonNode jsonNode = mapper.readTree(secret);
        log.info("Retrieved secrets from aws secret manager:{}",secret);
        return jsonNode;
    }
}
