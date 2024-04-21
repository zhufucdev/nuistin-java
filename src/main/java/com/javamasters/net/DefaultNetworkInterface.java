package com.javamasters.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class DefaultNetworkInterface {
    public static NetworkInterface get() {
        try {
            var localhost = InetAddress.getLocalHost();
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                var curr = interfaces.nextElement();
                if (curr.getInterfaceAddresses().stream().anyMatch(ia -> ia.getAddress().equals(localhost))) {
                    return curr;
                }
            }
        } catch (UnknownHostException | SocketException e) {
            // ignored
        }
        return null;
    }
}
