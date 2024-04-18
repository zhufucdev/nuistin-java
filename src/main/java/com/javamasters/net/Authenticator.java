package com.javamasters.net;

import com.javamasters.model.Account;
import com.javamasters.model.LoginRequest;
import com.javamasters.model.LoginResponse;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.AsyncSubject;

import java.net.*;
import java.net.http.HttpRequest;

public class Authenticator {
    private final AsyncHttpClient httpClient;
    private final String authUrl;
    private final NetworkInterface nic;

    private final AsyncSubject<State> state;

    public Authenticator(String authUrl, String pingAddress, NetworkInterface networkInterface) {
        this(authUrl, networkInterface);

        dispatchPing(pingAddress)
                .subscribeOn(Schedulers.io())
                .subscribe(pong -> {
                    if (state.getValue() == null) {
                        state.onNext(State.Online);
                        state.onComplete();
                    }
                });
    }

    public Authenticator(String authUrl, NetworkInterface networkInterface) {
        this.authUrl = authUrl;
        httpClient = new AsyncHttpClient();
        nic = networkInterface;
        state = AsyncSubject.create();
    }

    public Observable<State> getState() {
        return state;
    }

    public Single<Boolean> login(Account account) {
        var single = dispatchLoginRequest(account)
                .concatMap(req -> {
                    var loginRequest = HttpRequest.newBuilder()
                            .uri(URI.create(authUrl + "/api/v1/login"))
                            .POST(JsonDelegating.bodyPublisher(req))
                            .build();
                    return httpClient.send(loginRequest, JsonDelegating.bodyHandler(LoginResponse.class));
                })
                .map(res -> res.code() == 200);
        single.subscribe(loggedIn -> {
            if (loggedIn) {
                state.onNext(State.Online);
            } else {
                state.onNext(State.Unauthenticated);
            }
            state.onComplete();
        });
        return single;
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

    private Single<Boolean> dispatchPing(String address) {
        return Single.create((SingleEmitter<Boolean> emitter) -> {
            try {
                var addr = InetAddress.getByName(address);
                emitter.onSuccess(addr.isReachable(10000));
            } catch (UnknownHostException e) {
                emitter.onSuccess(false);
            }
        });
    }

    public enum State {
        Unspecified, Online, Unauthenticated, Offline
    }
}
