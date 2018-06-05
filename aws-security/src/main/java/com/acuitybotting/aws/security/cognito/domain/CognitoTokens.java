package com.acuitybotting.aws.security.cognito.domain;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Created by Zachary Herridge on 6/4/2018.
 */
@Data
@Builder
public class CognitoTokens {

    private String idToken;
    private String accessToken;
    private String refreshToken;

}
