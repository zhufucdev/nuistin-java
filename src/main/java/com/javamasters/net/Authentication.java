package com.javamasters.net;

import com.javamasters.model.Account;
import com.javamasters.model.LoginRequest;
import com.javamasters.model.LoginResponse;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.AsyncSubject;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpRequest;

public class Authentication {
    private final AsyncHttpClient httpClient;
    private final String authUrl;
    private final NetworkInterface nic;

    public final Observable<State> state;

    public Authentication(String authUrl, NetworkInterface networkInterface) {
        this.authUrl = authUrl;
        httpClient = new AsyncHttpClient();
        nic = networkInterface;
        state = AsyncSubject.create();
    }

    public Single<Boolean> login(Account account) {
        return dispatchLoginRequest(account)
                .concatMap(req -> {
                    var loginRequest = HttpRequest.newBuilder()
                            .uri(URI.create(authUrl + "/api/v1/login"))
                            .POST(JsonDelegating.bodyPublisher(req))
                            .build();
                    return httpClient.send(loginRequest, JsonDelegating.bodyHandler(LoginResponse.class));
                })
                .map(res -> res.code() == 200);
    }

    private Single<LoginRequest> dispatchLoginRequest(Account account) {
        var iter = nic.getInetAddresses();
        if (!iter.hasMoreElements()) {
            return Single.error(new NullPointerException("No ip addresses found"));
        }
        var addr = iter.nextElement();
        while (iter.hasMoreElements()) {
            if (addr instanceof Inet4Address) {
                break;
            }
            addr = iter.nextElement();
        }
        if (!(addr instanceof Inet4Address)) {
            return Single.error(new IllegalArgumentException("selected network interface has no ipv4 addresses"));
        }

        return Single.just(new LoginRequest(
                account.id(), account.password(),
                "0", String.valueOf(account.isp().channel),
                "secondauth", addr.getHostAddress()
        ));
    }

    public enum State {
        Unspecified, Online, Unauthenticated, Offline
    }
}
