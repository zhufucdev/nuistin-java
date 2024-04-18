package com.javamasters.net;

import com.google.gson.Gson;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class JsonDelegating {
    private static final Gson gson = new Gson();

    public static <T> HttpResponse.BodyHandler<T> bodyHandler(Class<T> clazz) {
        return responseInfo -> HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8),
                (str) -> gson.fromJson(str, clazz)
        );
    }

    public static HttpRequest.BodyPublisher bodyPublisher(Object data) {
        return HttpRequest.BodyPublishers.ofString(gson.toJson(data));
    }
}
