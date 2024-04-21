package com.javamasters.net;

import com.javamasters.net.model.LoginRequest;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.Charset;

public class PostLoginRequest<T> implements Function<LoginRequest, Single<T>> {
    private final String authUrl;
    private final AsyncHttpClient httpClient;
    private final Class<T> responseClass;

    private PostLoginRequest(String authUrl, AsyncHttpClient httpClient, Class<T> responseClass) {
        this.authUrl = authUrl;
        this.httpClient = httpClient;
        this.responseClass = responseClass;
    }

    @Override
    public Single<T> apply(LoginRequest req) {
        var loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(authUrl + "/api/v1/login"))
                .POST(JsonDelegating.bodyPublisher(req))
                .build();
        return httpClient.send(loginRequest, JsonDelegating.bodyHandler(responseClass, Charset.forName("GBK")));
    }

    public static <K> PostLoginRequest<K> of(String authUrl, AsyncHttpClient httpClient, Class<K> responseClass) {
        return new PostLoginRequest<>(authUrl, httpClient, responseClass);
    }
}
