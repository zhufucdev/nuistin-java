package com.javamasters.data;

import com.javamasters.i18n.Resources;

public interface Library {
    AccountProvider getAccountProvider();
    Settings getSettings();
    Resources getResources();
}
