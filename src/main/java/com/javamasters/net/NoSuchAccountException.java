package com.javamasters.net;

import com.javamasters.net.model.LoginResponse;

public class NoSuchAccountException extends Exception {
    public final LoginResponse response;

    public NoSuchAccountException(LoginResponse response) {
        super("Response: " + response);
        this.response = response;
    }
}
