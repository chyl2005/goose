package com.mm.goose.client.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(IPUtil.class);

    /**
     * 获取本级ip地址
     *
     * @return
     */
    public static String getLocatAddress() {
        String first = null;
        try {
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (inetAddress instanceof Inet4Address) {
                        if (first == null) {
                            first = inetAddress.getHostAddress();
                        }
                        if ("eth0".equals(networkInterface.getName())) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
            return first;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    public static String getAddress() {
        String first = null;
        try {
            String eth = "eth0";
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface
                    .getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface
                        .isVirtual() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface
                        .getInetAddresses();
                //add by liuyunpeng

                String addr = null;
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (inetAddress != null
                            && inetAddress instanceof Inet4Address) {
                        if (first == null) {
                            first = inetAddress.getHostAddress();
                        }
                        addr = inetAddress.getHostAddress();
                    }
                }
                if (networkInterface.getName() != null && networkInterface.getName().startsWith("eth")) {
                    if (networkInterface.getName().compareTo(eth) >= 0) {
                        first = addr;
                        eth = networkInterface.getName();
                    }
                }
            }
            return first;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    /**
     * 获取本机Host
     *
     * @return
     */
    public static String getLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }
}
