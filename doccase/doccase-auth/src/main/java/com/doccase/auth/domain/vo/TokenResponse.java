package com.doccase.auth.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String tokenType;
    private boolean mfaRequired;

    public static TokenResponse of(String accessToken, String refreshToken, Long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, expiresIn, "Bearer", false);
    }

    public static TokenResponse mfaRequired() {
        TokenResponse response = new TokenResponse();
        response.setMfaRequired(true);
        return response;
    }
}
