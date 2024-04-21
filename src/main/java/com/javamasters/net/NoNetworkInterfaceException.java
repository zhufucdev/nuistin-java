package com.javamasters.net;

public class NoNetworkInterfaceException extends Exception {
    public NoNetworkInterfaceException() {
    }

    public NoNetworkInterfaceException(String message) {
        super(message);
    }

    public NoNetworkInterfaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoNetworkInterfaceException(Throwable cause) {
        super(cause);
    }

    public NoNetworkInterfaceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
