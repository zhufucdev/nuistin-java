package com.javamasters.net;

import io.reactivex.rxjava3.core.Single;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AsyncHttpClient {
    private final HttpClient client;

    public AsyncHttpClient() {
       client = HttpClient.newBuilder().build();
    }

    public AsyncHttpClient(HttpClient.Builder builder) {
        client = builder.build();
    }

    public <T> Single<T> send(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
        return Single.create(emitter -> client.sendAsync(request, bodyHandler)
                .thenApply(HttpResponse::body)
                .whenComplete((t, error) -> {
                    if (error == null) {
                        emitter.onSuccess(t);
                    } else {
                        emitter.onError(error);
                    }
                })
        );
    }
}
