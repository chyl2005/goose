package com.mm.goose.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import com.mm.goose.netty.config.Command;
import com.mm.goose.netty.config.CommandType;

/**
 * @author:chyl2005
 * @date:17/11/1
 * @time:15:14
 * @desc:描述该类的作用
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<Command> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRemotingServer.class);
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                                                                                  Runtime.getRuntime().availableProcessors(),
                                                                                  600L,
                                                                                  TimeUnit.SECONDS,
                                                                                  new ArrayBlockingQueue<Runnable>(65536));

    private Map<String, Object> rpcServiceMap;

    public NettyServerHandler(Map<String, Object> rpcServiceMap) {
        this.rpcServiceMap = rpcServiceMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command command) throws Exception {

        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                LOGGER.debug("NettyServerHandler receive request " + command.getRequestId());
                try {
                    Object result = handle(command);
                    command.setResult(result);
                } catch (Throwable t) {
                    command.setError(t.toString());
                    LOGGER.error("NettyServerHandler handle request error", t);
                }
                command.setType(CommandType.RESPONSE);
                ctx.writeAndFlush(command).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        LOGGER.debug("NettyServerHandler Send response for request " + command.getRequestId());
                    }
                });
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        final String remoteAddress = ctx.channel().remoteAddress().toString();
        LOGGER.error("NettyServerHandler PIPELINE: exceptionCaught {}  exception.",remoteAddress, cause);
        ctx.channel().close();
    }

    private Object handle(Command request) throws Throwable {
        String className = request.getClassName();
        Object serviceBean = rpcServiceMap.get(className);
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        LOGGER.debug(serviceClass.getName());
        LOGGER.debug(methodName);
        for (int i = 0; i < parameterTypes.length; ++i) {
            LOGGER.debug(parameterTypes[i].getName());
        }
        for (int i = 0; i < parameters.length; ++i) {
            LOGGER.debug(parameters[i].toString());
        }
        // Cglib reflect
        FastClass serviceFastClass = FastClass.create(serviceClass);
        FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);
        return serviceFastMethod.invoke(serviceBean, parameters);
    }

}
