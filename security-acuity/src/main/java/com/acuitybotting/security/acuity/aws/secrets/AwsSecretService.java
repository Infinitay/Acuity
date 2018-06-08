package com.acuitybotting.security.acuity.aws.secrets;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.InvalidRequestException;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import java.security.InvalidParameterException;
import java.util.Optional;

/**
 * Created by Zachary Herridge on 6/1/2018.
 */
@Service
public class AwsSecretService {

    public <T> Optional<T> getSecret(String endpoint, String region, String secretName, Class<T> tClass) {
        return getSecret(endpoint, region, secretName).map(s -> new Gson().fromJson(s, tClass));
    }

    public Optional<String> getSecret(String endpoint, String region, String secretName) {
        if (System.getenv(secretName) != null) return Optional.ofNullable(System.getenv(secretName));

        AwsClientBuilder.EndpointConfiguration config = new AwsClientBuilder.EndpointConfiguration(endpoint, region);
        AWSSecretsManagerClientBuilder clientBuilder = AWSSecretsManagerClientBuilder.standard();
        clientBuilder.setEndpointConfiguration(config);
        AWSSecretsManager client = clientBuilder.build();

        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretName);
        GetSecretValueResult getSecretValueResponse = null;
        try {
            getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
        }
        catch (ResourceNotFoundException e) {
            System.out.println("The requested secret " + secretName + " was not found");
        }
        catch (InvalidRequestException e) {
            System.out.println("The request was invalid due to: " + e.getMessage());
        }
        catch (InvalidParameterException e) {
            System.out.println("The request had invalid params: " + e.getMessage());
        }

        if (getSecretValueResponse == null) return Optional.empty();
        return Optional.ofNullable(getSecretValueResponse.getSecretString());
    }

}
