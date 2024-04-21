package com.javamasters.net;

import com.javamasters.model.Account;
import com.javamasters.net.model.LoginRequest;
import com.javamasters.net.model.LoginResponse;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

import java.net.*;
import java.util.Objects;

public class Authenticator implements Disposable {
    private final AsyncHttpClient httpClient = new AsyncHttpClient();
    private final String authUrl;
    private String pingAddress;
    private final NetworkInterface nic;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final BehaviorSubject<State> state = BehaviorSubject.create();
    private Account account;

    public Authenticator(String authUrl, String pingAddress, NetworkInterface networkInterface) {
        this(authUrl, networkInterface);
        this.pingAddress = pingAddress;
        tryDisposePing();
    }

    public Authenticator(String authUrl, NetworkInterface networkInterface) {
        Objects.requireNonNull(authUrl, "Authentication server cannot be null");
        Objects.requireNonNull(networkInterface, "NetworkInterface cannot be null");
        this.authUrl = authUrl;
        nic = networkInterface;
        state.onNext(State.Unspecified);
    }

    private void tryDisposePing() {
        if (pingAddress != null) {
            var ping = dispatchPing(pingAddress)
                    .subscribeOn(Schedulers.io())
                    .subscribe(state::onNext);
            disposables.add(ping);
        }
    }

    public Observable<State> getState() {
        return state;
    }

    public Single<Boolean> login(Account account) {
        state.onNext(State.Unspecified);
        return dispatchVerifyRequest(account)
                .flatMap(PostLoginRequest.of(authUrl, httpClient, LoginResponse.class))
                .flatMap(res -> {
                    if (res.code() != 200) {
                        return Single.error(new NoSuchAccountException(res));
                    }
                    return dispatchLoginRequest(account);
                })
                .flatMap(PostLoginRequest.of(authUrl, httpClient, LoginResponse.class))
                .map(res -> res.code() == 200)
                .doOnSuccess(loggedIn -> {
                    if (loggedIn) {
                        state.onNext(State.Online);
                        this.account = account;
                    } else {
                        state.onNext(State.Unauthenticated);
                    }
                })
                .doOnError(throwable -> tryDisposePing());
    }

    public Single<Boolean> logout(Account account) {
        if (account == null) {
            return Single.error(new IllegalStateException("Never logged in"));
        }
        state.onNext(State.Unspecified);
        return dispatchVerifyRequest(account)
                .flatMap(PostLoginRequest.of(authUrl, httpClient, LoginResponse.class))
                .flatMap(res -> {
                    if (res.code() != 200) {
                        return Single.error(new NoSuchAccountException(res));
                    }
                    return dispatchLogoutRequest(account);
                })
                .flatMap(PostLoginRequest.of(authUrl, httpClient, LoginResponse.class))
                .map(res -> res.code() == 200)
                .doOnSuccess(loggedOut -> {
                    if (loggedOut) {
                        state.onNext(State.Unauthenticated);
                    }
                })
                .doOnError(throwable -> tryDisposePing());
    }

    public Single<Boolean> logout() {
        return logout(account);
    }

    private Single<LoginRequest> dispatchLoginBase(Account account, String channel, String pagesign, String ifautologin) {
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
                ifautologin, channel,
                pagesign, addr.getHostAddress()
        ));
    }

    private Single<LoginRequest> dispatchVerifyRequest(Account account) {
        return dispatchLoginBase(account, "_GET", "firstauth", "0");
    }

    private Single<LoginRequest> dispatchLoginRequest(Account account) {
        return dispatchLoginBase(account, String.valueOf(account.isp().channel), "secondauth", "0");
    }

    private Single<LoginRequest> dispatchLogoutRequest(Account account) {
        return dispatchLoginBase(account, "0", "thirdauth", "1");
    }

    private Single<State> dispatchPing(String address) {
        return Single.create(emitter -> {
            if (Ping.canReach(address, nic)) {
                emitter.onSuccess(State.Online);
            } else {
                var host = URI.create(authUrl).getHost();
                emitter.onSuccess(Ping.canReach(host, nic) ? State.Unauthenticated : State.Offline);
            }
        });
    }

    @Override
    public void dispose() {
        disposables.dispose();
    }

    @Override
    public boolean isDisposed() {
        return disposables.isDisposed();
    }

    public enum State {
        Unspecified, Online, Unauthenticated, Offline
    }
}
