package com.javamasters.net.model;

public record LogoutResponse(short code, String message, LogoutResponseData data) {
}
