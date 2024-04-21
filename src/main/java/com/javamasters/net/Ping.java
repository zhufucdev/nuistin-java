package com.javamasters.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;

public class Ping {
    public static boolean canReach(String host, NetworkInterface nic) {
        try {
            var addr = InetAddress.getByName(host);
            return addr.isReachable(nic, 32, 5000);
        } catch (IOException e) {
            return false;
        }
    }
}
