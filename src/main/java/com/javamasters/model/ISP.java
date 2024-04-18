package com.javamasters.model;

public enum ISP {
    Campus(1), Mobile(2), Telecom(3), Unicom(4);

    public final int channel;
    ISP(int channel) {
        this.channel = channel;
    }
}
