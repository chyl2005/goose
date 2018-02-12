package com.mm.goose.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mm.goose.netty.codec.NettyDecoder;
import com.mm.goose.netty.codec.NettyEncoder;
import com.mm.goose.netty.config.Command;
import com.mm.goose.netty.config.NettyClientConfig;
import com.mm.goose.netty.exception.RemotingException;
import com.mm.goose.netty.utils.AddressUtils;

/**
 * @author:chyl2005
 * @date:17/11/1
 * @time:14:52
 * @desc:描述该类的作用
 */
public class NettyRemotingClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRemotingClient.class);

    private ConcurrentHashMap<String, RPCFuture> pendingRPC = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<>();
    private static NettyRemotingClient remotingClient = new NettyRemotingClient();
    private static NettyClientConfig nettyClientConfig = new NettyClientConfig();
    public int sendPort = -1;
    private Random random = new Random();

    private final Bootstrap bootstrap = new Bootstrap();
    protected EventLoopGroup eventLoopGroup;

    private DefaultEventExecutorGroup defaultEventExecutorGroup = new DefaultEventExecutorGroup(32, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "NettyRemotingClientThread_" + this.threadIndex.incrementAndGet());
        }
    });

    public static NettyRemotingClient getInstance() {
        return remotingClient;
    }

    public NettyRemotingClient() {

        eventLoopGroup = new NioEventLoopGroup(1, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("NettyRemotingClientThread_%d", this.threadIndex.incrementAndGet()));
            }
        });
    }

    @PostConstruct
    public void start() {
        if (sendPort > 0) {
            this.nettyClientConfig.setConnectPort(sendPort);
        }

        this.bootstrap.group(this.eventLoopGroup).channel(NioSocketChannel.class)//
                      //
                      .option(ChannelOption.TCP_NODELAY, true)
                      //
                      .option(ChannelOption.SO_SNDBUF, nettyClientConfig.getSocketSndbufSize())
                      //
                      .option(ChannelOption.SO_RCVBUF, nettyClientConfig.getSocketSndbufSize())
                      //
                      .handler(new ChannelInitializer<SocketChannel>() {
                          @Override
                          public void initChannel(SocketChannel ch) throws Exception {
                              ch.pipeline().addLast(
                                      defaultEventExecutorGroup,
                                      new NettyEncoder(Command.class),
                                      new NettyDecoder(Command.class),
                                      new NettyClientHandler());
                          }
                      });
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("Shutdown netty remoting client...");
        try {
            for (Channel cw : this.channels.values()) {
                cw.close();
            }
            this.channels.clear();
            this.eventLoopGroup.shutdownGracefully();

            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            LOGGER.error("NettyRemotingClient shutdown exception, ", e);
        }

    }

    public RPCFuture send(Command command) {
        String node = chooseNode();
        return remotingClient.send(node, command);
    }

    public String chooseNode() {
        List<String> nodes = new ArrayList<>(this.channels.keySet());
        return nodes.get(getRandomInt(nodes.size()));

    }

    /**
     * 获得一个[0,max)之间的整数。
     *
     * @param max
     * @return
     */
    public int getRandomInt(int max) {
        return Math.abs(random.nextInt()) % max;
    }

    public RPCFuture send(String address, final Command command) throws RemotingException {
        Channel channel = getChannel(address);
        if (channel == null) {
            LOGGER.error("Please check  specified target address. If the address is ok, check network.");
            throw new RemotingException("Network encounter error!");
        }
        try {
            RPCFuture rpcFuture = new RPCFuture(command);
            pendingRPC.put(command.getRequestId(), rpcFuture);
            channel.writeAndFlush(command);
            ChannelFuture future = channel.writeAndFlush(command).await();
            if (future.isSuccess()) {
                LOGGER.info("Command : {} Send successfully.", command.toString());
                return rpcFuture;
            }
            LOGGER.info("Command : {} Send failed. caused by :", command.toString(), future.cause());
            throw new RemotingException("Send command: + " + command.toString() + ",to " + "address:" + address + "failed.");

        } catch (Exception e) {
            LOGGER.error("Send command {} to address {} encounter error.", command, address);
            throw new RemotingException("Send command: + " + command + ",to " + "address:" + address + "encounter error.", e);
        }
    }

    private void closeChannels() {

        for (Channel cw : this.channels.values()) {
            cw.close();
        }
        this.channels.clear();
    }

    /**
     * 更新服务节点
     *
     * @param nodes
     */
    public void updateServerNodes(Set<String> nodes) {
        for (String node : nodes) {
            String[] splits = node.split(":");
            String address = splits[0];
            String port = splits[1];
            getChannel(address, Integer.valueOf(port));
        }
    }

    public Channel getChannel(String address, int port) {
        if (address == null) {
            return null;
        }
        Channel channel = channels.get(address + ":" + port);
        if (channel == null || !channel.isActive()) {
            return createNewChannel(address, port);
        }
        return channel;
    }

    public Channel getChannel(String address) {
        return getChannel(address, nettyClientConfig.getConnectPort());
    }

    private Channel createNewChannel(String address, int port) {
        ChannelFuture future;
        try {
            future = bootstrap.connect(new InetSocketAddress(address, port)).await();
        } catch (Exception e) {
            LOGGER.info("Connect to TargetServer encounter error.", e);
            return null;
        }
        if (future.isSuccess()) {
            Channel channel = future.channel();
            channels.put(address + ":" + port, channel);
            return channel;
        } else {
            LOGGER.error("Connect to TargetServer failed.", future.cause());
            return null;
        }
    }

    public void removeChannel(String address, int port) {
        if (StringUtils.isNotBlank(address)) {
            channels.remove(address + ":" + port);
        }
    }

    private class NettyClientHandler extends SimpleChannelInboundHandler<Command> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, Command response) throws Exception {
            String requestId = response.getRequestId();
            RPCFuture rpcFuture = pendingRPC.get(requestId);
            if (rpcFuture != null) {
                pendingRPC.remove(requestId);
                rpcFuture.done(response);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            String remoteAddress = AddressUtils.parseChannelRemoteAddr1(ctx.channel());
            channels.remove(remoteAddress);
            LOGGER.error("client caught exception", cause);
            ctx.close();
        }

    }
}