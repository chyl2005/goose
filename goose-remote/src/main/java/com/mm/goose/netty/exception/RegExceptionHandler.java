package com.mm.goose.netty.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class RegExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RegExceptionHandler.class);

    private RegExceptionHandler() {
    }

    /**
     * 处理掉中断和连接失效异常并继续抛出RegException.
     *
     * @param cause 待处理的异常.
     */
    public static void handleException(final Exception cause) {
        log.error("Reg Exception:", cause);
        throw new RegException(cause);
    }

}
