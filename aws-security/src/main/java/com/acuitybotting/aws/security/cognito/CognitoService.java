package com.acuitybotting.aws.security.cognito;

import com.acuitybotting.aws.security.cognito.domain.CognitoConfiguration;
import com.acuitybotting.aws.security.cognito.domain.CognitoTokens;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClientBuilder;
import com.amazonaws.services.cognitoidentity.model.*;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Created by Zachary Herridge on 6/4/2018.
 */
@Service
public class CognitoService {

    public Optional<CognitoTokens> login(CognitoConfiguration cognitoConfiguration, String username, String password){
        try {
            SRPAuthentication helper = new SRPAuthentication(cognitoConfiguration.getPoolId(), cognitoConfiguration.getClientAppId(), cognitoConfiguration.getSecretKey(), cognitoConfiguration.getRegion());
            RespondToAuthChallengeResult result = helper.performSRPAuthentication(username, password);
            if (result != null){
                return Optional.of(CognitoTokens.builder()
                        .accessToken(result.getAuthenticationResult().getAccessToken())
                        .refreshToken(result.getAuthenticationResult().getRefreshToken())
                        .idToken(result.getAuthenticationResult().getIdToken())
                        .build());
            }
        }
        catch (Exception e){
            throw new RuntimeException("Exception during login.", e);
        }

        return Optional.empty();
    }

    public void getUser(CognitoConfiguration cognitoConfiguration, String accessToken){
        AnonymousAWSCredentials awsCreds = new AnonymousAWSCredentials();
        AWSCognitoIdentityProvider cognitoIdentityProvider = AWSCognitoIdentityProviderClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.fromName(cognitoConfiguration.getRegion()))
                .build();

        GetUserResult user = cognitoIdentityProvider.getUser(new GetUserRequest().withAccessToken(accessToken));
        System.out.println();
    }

    public CognitoTokens refresh(CognitoConfiguration cognitoConfiguration, CognitoTokens loginResult){
        AnonymousAWSCredentials awsCreds = new AnonymousAWSCredentials();
        AWSCognitoIdentityProvider cognitoIdentityProvider = AWSCognitoIdentityProviderClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.fromName(cognitoConfiguration.getRegion()))
                .build();

        InitiateAuthRequest initiateAuthRequest = new InitiateAuthRequest()
                .withAuthFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                .withClientId(cognitoConfiguration.getClientAppId())
                .addAuthParametersEntry("REFRESH_TOKEN", loginResult.getRefreshToken());

        InitiateAuthResult initiateAuthResult = cognitoIdentityProvider.initiateAuth(initiateAuthRequest);

        return CognitoTokens.builder()
                .accessToken(initiateAuthResult.getAuthenticationResult().getAccessToken())
                .idToken(initiateAuthResult.getAuthenticationResult().getIdToken())
                .refreshToken(loginResult.getRefreshToken())
                .build();
    }

    public Optional<Credentials> getCredentials(CognitoConfiguration cognitoConfiguration, CognitoTokens loginResult){
        try {
            AnonymousAWSCredentials awsCreds = new AnonymousAWSCredentials();

            AmazonCognitoIdentity provider = AmazonCognitoIdentityClientBuilder
                    .standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .withRegion(Regions.fromName(cognitoConfiguration.getRegion()))
                    .build();

            String providerUrl = CognitoJWTParser.getPayload(loginResult.getIdToken()).get("iss").toString().replace("https://", "");

            GetIdRequest idRequest = new GetIdRequest();
            idRequest.setIdentityPoolId(cognitoConfiguration.getFedPoolId());
            idRequest.addLoginsEntry(providerUrl, loginResult.getIdToken());
            GetIdResult idResult = provider.getId(idRequest);

            GetCredentialsForIdentityRequest request = new GetCredentialsForIdentityRequest();
            request.setIdentityId(idResult.getIdentityId());
            request.addLoginsEntry(providerUrl, loginResult.getIdToken());

            GetCredentialsForIdentityResult result = provider.getCredentialsForIdentity(request);
            return Optional.ofNullable(result.getCredentials());
        }
        catch (Exception e){
            throw new RuntimeException("Exception during credential acquisition.", e);
        }
    }
}
