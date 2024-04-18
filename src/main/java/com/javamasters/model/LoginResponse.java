package com.javamasters.model;

public record LoginResponse(short code, String message, LoginResponseData data) {
}
