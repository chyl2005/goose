package com.mm.goose.netty.utils;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddressUtils.class);
    public static String parseChannelRemoteAddr(final Channel channel) {
        if (null == channel) {
            return "";
        }
        final SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }
        return "";
    }

    public static String parseChannelRemoteAddr1(Channel channel) {

        if(channel == null){
            LOGGER.info("channel is null");
            return "";
        }

        SocketAddress socketAddress = channel.remoteAddress();
        if (socketAddress == null) {
            LOGGER.info("socketAddress is null");
            return "";
        }

        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress addr = (InetSocketAddress)socketAddress;
            String remoteHostAddr = addr.getAddress().getHostAddress();
            int remoteHostPort = addr.getPort();
            return remoteHostAddr + ":" + remoteHostPort;
        } else {
            LOGGER.info("RemoteAddress is not InetSocketAddress");
            return parseChannelRemoteAddr(channel);
        }
    }

}
