package com.mm.goose.netty.exception;

/**
 * @author:chyl2005
 * @date:17/11/2
 * @time:16:33
 * @desc:描述该类的作用
 */
public class RemotingException extends  RuntimeException{



    private static final long serialVersionUID = -1L;


    public RemotingException(String message) {
        super(message);
    }


    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }
}
