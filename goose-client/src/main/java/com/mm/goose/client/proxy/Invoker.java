package com.mm.goose.client.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import com.mm.goose.client.ServiceManager;
import com.mm.goose.netty.NettyRemotingClient;
import com.mm.goose.netty.config.Command;

/**
 * @author:chyl2005
 * @date:17/11/7
 * @time:15:19
 * @desc:描述该类的作用
 */
public class Invoker<T> implements InvocationHandler {


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()) {
            String name = method.getName();
            if ("equals".equals(name)) {
                return proxy == args[0];
            } else if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            } else if ("toString".equals(name)) {
                return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy)) + ", with InvocationHandler " + this;
            } else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }
        Command request = new Command();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        NettyRemotingClient remotingClient = NettyRemotingClient.getInstance();
        remotingClient.send(request);
        return null;
    }
}
