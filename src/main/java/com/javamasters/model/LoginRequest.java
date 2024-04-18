package com.javamasters.model;

public record LoginRequest(String username, String password, String ifautologin, String channel, String pagesign,
                           String useripadd) {
}
