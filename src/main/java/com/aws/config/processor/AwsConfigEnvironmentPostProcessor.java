package com.aws.config.processor;

import com.aws.config.constants.AwsConfigConstants;
import com.aws.config.service.ParameterStore;
import com.aws.config.service.SecretManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
public class AwsConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        log.info("Starting AwsConfigEnvironmentPostProcessor to update key values of application properties file");
        MutablePropertySources mutablePropertySources = environment.getPropertySources();
        boolean awsConfigEnabled = false;
        Map<String, Object> parameterStoreProperties = new HashMap<>();
        Map<String, Object> secretsManagerProperties = new HashMap<>();

        for (PropertySource<?> propertySource : mutablePropertySources) {
            if (propertySource instanceof EnumerablePropertySource<?>) {
                for (String key : ((EnumerablePropertySource<?>) propertySource).getPropertyNames()) {
                    if (AwsConfigConstants.AWS_CONFIG_ENABLED.equals(key) && String.valueOf(true).equals(propertySource.getProperty(key))) {
                        awsConfigEnabled = true;
                    }
                    if (key.startsWith(AwsConfigConstants.AWS_PARAMETER_STORE_PREFIX)) {
                        parameterStoreProperties.put(key, propertySource.getProperty(key));
                    }
                    if (key.startsWith(AwsConfigConstants.AWS_SECRET_MANAGER_PREFIX)) {
                        secretsManagerProperties.put(key, propertySource.getProperty(key));
                    }
                }
            }
        }

        if (!awsConfigEnabled) return;

        // Get values for Resolving the config properties
        Map<String, Object> parameterStoreSecretNames = parameterStoreProperties.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(AwsConfigConstants.AWS_PARAMETER_STORE_SECRET_NAME_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Object> awsProps = new HashMap<>();
        awsProps.putAll(configureAwsParameterStoreProps(parameterStoreSecretNames, parameterStoreProperties));
        log.info("ParameterStore AWS Properties:{}", awsProps);

        for (Map.Entry<String, Object> secretsEntry : secretsManagerProperties.entrySet()) {
            String secretKey = secretsEntry.getKey();
            if (secretKey.startsWith(AwsConfigConstants.AWS_SECRETS_MANAGER_SECRET_NAME_PREFIX)) {
                String keySuffix = secretKey.substring(AwsConfigConstants.AWS_SECRETS_MANAGER_SECRET_NAME_PREFIX.length());
                String awsKey = AwsConfigConstants.AWS_PARAMETER_STORE_PREFIX + keySuffix;

                if (awsProps.containsKey(awsKey)) {
                    secretsManagerProperties.put(secretKey, awsProps.get(secretKey));
                }
            }
        }

        Map<String, Object> secretManagerSecretNames = secretsManagerProperties.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(AwsConfigConstants.AWS_SECRETS_MANAGER_SECRET_NAME_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.info("SecretManager AWS Properties:{}", awsProps);
        String parameterStoreRegion = awsProps.get(AwsConfigConstants.AWS_PARAMETER_STORE_SECRET_REGION).toString();
        awsProps.putAll(configureAwsSecretManagersProps(secretManagerSecretNames, parameterStoreRegion, secretsManagerProperties));

        environment.getPropertySources().addFirst(new MapPropertySource("AwsConfigEnvironmentPostProcessor", awsProps));
        log.info("Completed AwsConfigEnvironmentPostProcessor to update key values of application properties file:{}", awsProps);
    }


    /**
     * Configures AWS Parameter Store properties by retrieving values from AWS
     * and mapping them to the specified property keys.
     *
     * @param parameterStoreSecretName a map containing parameter store secret names
     * @param parameterStoreProps      a map containing property keys to be mapped with AWS Parameter Store values
     * @return a map containing the updated properties with values retrieved from AWS Parameter Store
     * @throws RuntimeException if there is an error processing JSON data
     */
    public Map<String, Object> configureAwsParameterStoreProps(Map<String, Object> parameterStoreSecretName, Map<String, Object> parameterStoreProps) {
        Map<String, Object> awsProps = new HashMap<>();
        try {
            for (Map.Entry<String, Object> entry : parameterStoreProps.entrySet()) {
                // Get ParameterStore Values form AWS
                JsonNode parameterStoreValues = ParameterStore.getParameterStoreValue(entry.getKey());
                // Map parameter store values to properties file key
                for (String key : parameterStoreProps.keySet()) {
                    JsonNode prop = parameterStoreValues.get(key.substring(AwsConfigConstants.AWS_PARAMETER_STORE_PREFIX.length()));
                    if (prop != null) {
                        log.debug("AwsConfigEnvironmentPostProcessor:: Overwriting application property value " + key + " : " + prop.asText());
                        awsProps.put(key, prop.asText());
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);

        }
        return awsProps;
    }


    /**
     * Configures AWS Secret Managers properties by retrieving secret values from AWS Secret Manager.
     *
     * @param secretManagersNames Map of secret manager names to their corresponding secret IDs.
     * @param region              AWS region where the secret manager is located.
     * @param secretManagerProps  Map of property keys to their corresponding secret manager property values.
     * @return Map of property keys to their corresponding secret manager property values.
     */

    public Map<String, String> configureAwsSecretManagersProps(Map<String, Object> secretManagersNames, String region, Map<String, Object> secretManagerProps) {
        Map<String, String> awsprops = new HashMap<>();
        SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder().region(Region.of(region)).build();
        try {
            for (Map.Entry<String, Object> entry : secretManagersNames.entrySet()) {
                //Get Secret values from AWS Secret Manager
                JsonNode secretManagerValues = SecretManager.getSecretValues(secretsManagerClient, entry.getValue().toString());
                String prefix = AwsConfigConstants.AWS_SECRET_MANAGER_PREFIX.concat(
                        entry.getKey().substring(AwsConfigConstants.AWS_SECRETS_MANAGER_SECRET_NAME_PREFIX.length())
                );
                for (String key : secretManagerProps.keySet()) {
                    if (key.startsWith(prefix)) {
                        String computedKey = key.substring(prefix.length() + 1);
                        String value = secretManagerValues.get(computedKey).asText();
                        if (value != null) {
                            log.debug("AwsConfigEnvironmentPostProcessor:: Overwriting applications property value " + key + " : " + value);
                        }
                        awsprops.put(key, value);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }
        return awsprops;
    }
}
