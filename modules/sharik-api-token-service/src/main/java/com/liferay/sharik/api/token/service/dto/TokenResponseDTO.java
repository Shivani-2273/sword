package com.liferay.sharik.api.token.service.dto;

public class TokenResponseDTO {
    private String token;
    private String expiration_date;

    public TokenResponseDTO(String token, String expiration_date) {
        this.token = token;
        this.expiration_date = expiration_date;
    }
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getExpiration_date() {
        return expiration_date;
    }

    public void setExpiration_date(String expiration_date) {
        this.expiration_date = expiration_date;
    }
}
