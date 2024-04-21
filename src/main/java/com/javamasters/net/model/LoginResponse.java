package com.javamasters.net.model;

import java.util.Map;

public record LoginResponse(short code, String message, Map<String, Object> data) {
}
