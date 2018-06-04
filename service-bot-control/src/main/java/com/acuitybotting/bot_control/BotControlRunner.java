package com.acuitybotting.bot_control;

import com.acuitybotting.aws.security.cognito.CognitoService;
import com.acuitybotting.aws.security.cognito.domain.CognitoConfig;
import com.acuitybotting.aws.security.cognito.domain.CognitoLoginResult;
import com.acuitybotting.bot_control.services.messaging.BotControlMessagingService;
import com.acuitybotting.db.arango.bot_control.repositories.BotInstanceRepository;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.SQSActions;
import com.amazonaws.auth.policy.conditions.ConditionFactory;
import com.amazonaws.auth.policy.conditions.StringCondition;
import com.amazonaws.services.cognitoidentity.model.Credentials;
import com.amazonaws.services.cognitoidp.model.ListUsersRequest;
import com.amazonaws.services.sns.model.AddPermissionRequest;
import com.amazonaws.services.sqs.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Created by Zachary Herridge on 6/1/2018.
 */
@Component
public class BotControlRunner implements CommandLineRunner{

    private final BotControlMessagingService service;
    private final CognitoService cognitoService;
    private final BotInstanceRepository repository;

    @Autowired
    public BotControlRunner(BotControlMessagingService service, CognitoService cognitoService, BotInstanceRepository repository) {
        this.service = service;
        this.cognitoService = cognitoService;
        this.repository = repository;
    }

    @Override
    public void run(String... strings) throws Exception {
        CognitoConfig acuitybotting = CognitoConfig.builder()
                .poolId("us-east-1_HrbYmVhlY")
                .clientappId("3pgbd576sg70tsub4nh511k58u")
                .fedPoolId("us-east-1:ff1b33f4-7f66-47a5-b7ff-9696b0e1fb52")
                .customDomain("acuitybotting")
                .region("us-east-1")
                .redirectUrl("https://rspeer.org/")
                .build();

        CognitoLoginResult zach = cognitoService.login(acuitybotting, "Zach", System.getenv("CognitoPassword")).orElseThrow(() -> new RuntimeException("Failed to login."));
        Credentials credentials = cognitoService.getCredentials(acuitybotting, zach).orElseThrow(() -> new RuntimeException("Failed to get creds."));
        service.connect("us-east-1", credentials);

        read(service.createQueue("testQueue2.fifo", "139.225.128.101").getQueueUrl());
    }

    private void read(String queueUrl){
        Executors.newSingleThreadExecutor().submit(() -> {
            while (true){
                ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest();
                receiveMessageRequest.withQueueUrl(queueUrl);
                receiveMessageRequest.withMaxNumberOfMessages(10);
                receiveMessageRequest.withWaitTimeSeconds(20);
                ReceiveMessageResult receiveMessageResult = service.getSQS().receiveMessage(receiveMessageRequest);

                for (Message message : receiveMessageResult.getMessages()) {
                    System.out.println("Got message: " + message.getBody());
                    service.getSQS().deleteMessage(new DeleteMessageRequest().withQueueUrl(queueUrl).withReceiptHandle(message.getReceiptHandle()));
                }
            }
        });
    }
}
