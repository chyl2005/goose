package com.mm.goose.netty;

/**
 *
 */
public interface AsyncRPCCallback {

    void success(Object result);

    void fail(Exception e);

}
