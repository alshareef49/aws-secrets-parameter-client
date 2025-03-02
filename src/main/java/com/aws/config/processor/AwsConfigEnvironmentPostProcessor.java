package com.aws.config.processor;

import com.aws.config.constants.AwsConfigConstants;
import com.aws.config.service.ParameterStore;
import com.aws.config.service.SecretManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Slf4j
public class AwsConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        log.info("Starting AwsConfigEnvironmentPostProcessor to update key values of application properties file");


    }

    public Map<String,Object> configureAwsParameterStoreProps(Map<String,Object> parameterStoreSecretName,Map<String,Object> parameterStoreProps){
        Map<String,Object> awsProps = new HashMap<>();
        try{
            for(Map.Entry<String,Object> entry:parameterStoreProps.entrySet()){
                // Get ParameterStore Values form AWS
                JsonNode parameterStoreValues = ParameterStore.getParameterStoreValue(entry.getKey());
                // Map parameter store values to properties file key
                for(String key:parameterStoreProps.keySet()){
                    JsonNode prop = parameterStoreValues.get(key.substring(AwsConfigConstants.AWS_PARAMETER_STORE_PREFIX.length()));
                    if(prop!=null){
                        log.debug("AwsConfigEnvironmentPostProcessor:: Overwriting application property value "+key+" : "+prop.asText());
                        awsProps.put(key,prop.asText());
                    }
                }
            }
        }catch (JsonProcessingException e){
            log.error(String.valueOf(e));
            throw new RuntimeException(e);

        }
        return awsProps;
    }

    public Map<String,String> configureAwsSecretManagersProps(Map<String,Object> secretManagersNames,String region,Map<String,Object> secretManagerProps){
        Map<String,String> awsprops = new HashMap<>();
        SecretsManagerClient secretsManagerClient = SecretsManagerClient.builder().region(Region.of(region)).build();
        try{
            for(Map.Entry<String,Object> entry:secretManagersNames.entrySet()){
                //Get Secret values from AWS Secret Manager
                JsonNode secretManagerValues = SecretManager.getSecretValues(secretsManagerClient,entry.getValue().toString());
                String prefix = AwsConfigConstants.AWS_SECRET_MANAGER_PREFIX.concat(
                        entry.getKey().substring(AwsConfigConstants.AWS_SECRETS_MANAGER_SECRET_NAME_PREFIX.length())
                );
                for(String key:secretManagerProps.keySet()){
                    if(key.startsWith(prefix)){
                        String computedKey = key.substring(prefix.length()+1);
                        String value = secretManagerValues.get(computedKey).asText();
                        if(value!=null){
                            log.debug("AwsConfigEnvironmentPostProcessor:: Overwriting applications property value "+key+" : "+value);
                        }
                        awsprops.put(key,value);
                    }
                }
            }
        }catch (JsonProcessingException e){
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }
        return awsprops;
    }
}
