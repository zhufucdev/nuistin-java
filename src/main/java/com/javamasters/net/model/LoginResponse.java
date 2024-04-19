package com.javamasters.net.model;

public record LoginResponse(short code, String message, LoginResponseData data) {
}
