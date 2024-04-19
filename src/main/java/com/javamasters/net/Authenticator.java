package com.javamasters.net;

import com.javamasters.model.Account;
import com.javamasters.net.model.LoginRequest;
import com.javamasters.net.model.LoginResponse;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.AsyncSubject;

import java.net.*;

public class Authenticator {
    private final AsyncHttpClient httpClient;
    private final String authUrl;
    private final NetworkInterface nic;

    private final AsyncSubject<State> state;
    private Account account;

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
        return dispatchLoginRequest(account)
                .concatMap(PostLoginRequest.of(authUrl, httpClient, LoginResponse.class))
                .map(res -> res.code() == 200)
                .doOnSuccess(loggedIn -> {
                    if (loggedIn) {
                        state.onNext(State.Online);
                        this.account = account;
                    } else {
                        state.onNext(State.Unauthenticated);
                    }
                    state.onComplete();
                });
    }

    public Single<Boolean> logout() {
        final var account = this.account;
        if (account == null) {
            return Single.error(new IllegalStateException("Never logged in"));
        }
        return dispatchLogoutRequest(account)
                .concatMap(PostLoginRequest.of(authUrl, httpClient, LoginResponse.class))
                .map(res -> res.code() == 200)
                .doOnSuccess(loggedOut -> {
                    if (loggedOut) {
                        state.onNext(State.Offline);
                        state.onComplete();
                    }
                });
    }

    private Single<LoginRequest> dispatchLoginBase(Account account, String pagesign, String ifautologin) {
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
                ifautologin, String.valueOf(account.isp().channel),
                pagesign, addr.getHostAddress()
        ));
    }

    private Single<LoginRequest> dispatchLoginRequest(Account account) {
        return dispatchLoginBase(account, "secondauth", "0");
    }

    private Single<LoginRequest> dispatchLogoutRequest(Account account) {
        return dispatchLoginBase(account, "thirdauth", "1");
    }

    private Single<Boolean> dispatchPing(String address) {
        return Single.create((SingleEmitter<Boolean> emitter) -> {
            try {
                var addr = InetAddress.getByName(address);
                emitter.onSuccess(addr.isReachable(nic, 32, 10000));
            } catch (UnknownHostException e) {
                emitter.onSuccess(false);
            }
        });
    }

    public enum State {
        Unspecified, Online, Unauthenticated, Offline
    }
}
