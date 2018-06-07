package com.acuitybotting.bot_control;

import com.acuitybotting.security.acuity.jwt.AcuityJwtService;
import com.acuitybotting.security.acuity.aws.cognito.CognitoAuthenticationService;
import com.acuitybotting.security.acuity.aws.cognito.domain.CognitoConfiguration;
import com.acuitybotting.security.acuity.aws.cognito.domain.CognitoTokens;
import com.acuitybotting.bot_control.services.messaging.BotControlMessagingService;
import com.acuitybotting.db.arango.bot_control.repositories.BotInstanceRepository;
import com.amazonaws.handlers.HandlerContextKey;
import com.amazonaws.services.cognitoidentity.model.Credentials;
import com.amazonaws.services.sqs.model.*;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * Created by Zachary Herridge on 6/1/2018.
 */
@Component
public class BotControlRunner implements CommandLineRunner{

    private final BotControlMessagingService service;
    private final CognitoAuthenticationService cognitoAuthenticationService;
    private final AcuityJwtService jwtService;
    private final BotInstanceRepository repository;

    @Autowired
    public BotControlRunner(BotControlMessagingService service, CognitoAuthenticationService cognitoAuthenticationService, AcuityJwtService jwtService, BotInstanceRepository repository) {
        this.service = service;
        this.cognitoAuthenticationService = cognitoAuthenticationService;
        this.jwtService = jwtService;
        this.repository = repository;
    }

    @Override
    public void run(String... strings) throws Exception {
        cognitoAuthenticationService.setCognitoConfiguration(
                CognitoConfiguration.builder()
                        .poolId("us-east-1_HrbYmVhlY")
                        .clientAppId("3pgbd576sg70tsub4nh511k58u")
                        .fedPoolId("us-east-1:ff1b33f4-7f66-47a5-b7ff-9696b0e1fb52")
                        .customDomain("acuitybotting")
                        .region("us-east-1")
                        .redirectUrl("https://rspeer.org/")
                        .build()
        );

        CognitoTokens cognitoTokens = cognitoAuthenticationService.login("Zach", System.getenv("CognitoPassword")).orElse(null);
        Credentials credentials = cognitoAuthenticationService.getCredentials(cognitoTokens).orElse(null);

        service.connect("us-east-1", credentials);
        service.getClientService().consumeQueue("https://sqs.us-east-1.amazonaws.com/604080725100/test.fifo", message -> System.out.println("Message: " + message));
    }
}
