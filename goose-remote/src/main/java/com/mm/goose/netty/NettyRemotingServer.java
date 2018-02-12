package com.mm.goose.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import com.mm.goose.netty.codec.NettyDecoder;
import com.mm.goose.netty.codec.NettyEncoder;
import com.mm.goose.netty.config.Command;
import com.mm.goose.netty.config.NettyServerConfig;

/**
 * @author:chyl2005
 * @date:17/11/1
 * @time:14:20
 * @desc:描述该类的作用
 */
public class NettyRemotingServer implements ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRemotingServer.class);
    public int listenPort = 8001;
    private NettyServerConfig nettyServerConfig;

    private ServerBootstrap serverBootstrap = new ServerBootstrap();
    private Map<String, Object> rpcServiceMap = new HashMap<>();
    private EventLoopGroup eventLoopGroupWorker;
    protected EventLoopGroup eventLoopGroup;

    protected DefaultEventExecutorGroup defaultEventExecutorGroup = new DefaultEventExecutorGroup(32, new ThreadFactory() {
        private AtomicInteger threadIndex = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "NettyClientWorkerThread_" + this.threadIndex.incrementAndGet());
        }
    });



    public NettyRemotingServer(NettyServerConfig nettyServerConfig) {
        this.nettyServerConfig = nettyServerConfig;

        this.nettyServerConfig = nettyServerConfig;
        this.eventLoopGroup = new NioEventLoopGroup(1, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("NettyRemotingServer%d", this.threadIndex.incrementAndGet()));
            }
        });

        this.eventLoopGroupWorker = new NioEventLoopGroup(nettyServerConfig.getServerSelectorThreads(), new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);
            private int threadTotal = nettyServerConfig.getServerSelectorThreads();

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, String.format("NettyRemotingServer%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
            }
        });
    }

    @PostConstruct
    public void start() {
        this.nettyServerConfig.setListenPort(listenPort);
        this.serverBootstrap.group(this.eventLoopGroup, this.eventLoopGroupWorker).channel(NioServerSocketChannel.class)
                            //
                            .option(ChannelOption.SO_BACKLOG, 1024)
                            //
                            .option(ChannelOption.SO_REUSEADDR, true)
                            //
                            .childOption(ChannelOption.TCP_NODELAY, true)
                            //
                            .childOption(ChannelOption.SO_SNDBUF, nettyServerConfig.getSocketSndbufSize())
                            //
                            .childOption(ChannelOption.SO_RCVBUF, nettyServerConfig.getSocketSndbufSize())

                            .localAddress(new InetSocketAddress(this.nettyServerConfig.getListenPort()))
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel socketChannel) throws Exception {
                                    socketChannel.pipeline().addLast(defaultEventExecutorGroup,
                                                                     new NettyEncoder(Command.class),
                                                                     new NettyDecoder(Command.class),
                                                                     new NettyServerHandler(rpcServiceMap));
                                }
                            });

        try {
            this.serverBootstrap.bind().sync();
        } catch (InterruptedException e1) {
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e1);
        }
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.info("Shutdown netty remoting server....");
        try {
            this.eventLoopGroup.shutdownGracefully();
            this.eventLoopGroupWorker.shutdownGracefully();
            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            LOGGER.error("NettyRemotingServer shutdown exception, ", e);
        }

    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceBean : serviceBeanMap.values()) {
                String interfaceName = serviceBean.getClass().getAnnotation(RpcService.class).value().getName();
                rpcServiceMap.put(interfaceName, serviceBean);
            }
        }
    }
}
