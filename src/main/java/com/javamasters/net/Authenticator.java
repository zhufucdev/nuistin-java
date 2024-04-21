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

public class Authenticator implements Disposable {
    private final AsyncHttpClient httpClient = new AsyncHttpClient();
    private final String authUrl;
    private final NetworkInterface nic;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final BehaviorSubject<State> state = BehaviorSubject.create();
    private Account account;

    public Authenticator(String authUrl, String pingAddress, NetworkInterface networkInterface) {
        this(authUrl, networkInterface);
        var ping = dispatchPing(pingAddress)
                .subscribeOn(Schedulers.io())
                .subscribe(state::onNext);
        disposables.add(ping);
    }

    public Authenticator(String authUrl, NetworkInterface networkInterface) {
        this.authUrl = authUrl;
        nic = networkInterface;
        state.onNext(State.Unspecified);
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
                });
    }

    public Single<Boolean> logout(Account account) {
        if (account == null) {
            return Single.error(new IllegalStateException("Never logged in"));
        }
        return dispatchLogoutRequest(account)
                .concatMap(PostLoginRequest.of(authUrl, httpClient, LoginResponse.class))
                .map(res -> res.code() == 200)
                .doOnSuccess(loggedOut -> {
                    if (loggedOut) {
                        state.onNext(State.Unauthenticated);
                    }
                });
    }

    public Single<Boolean> logout() {
        return logout(account);
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
