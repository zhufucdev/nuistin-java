package com.javamasters.data;

import io.reactivex.rxjava3.subjects.Subject;

public interface Settings {
    Subject<String> preferredLanguage();
    Subject<String> authServer();
    Subject<String> pingAddress();
    Subject<String> nic();
}
