package com.javamasters.model;

public record LogoutResponse(short code, String message, LogoutResponseData data) {
}
