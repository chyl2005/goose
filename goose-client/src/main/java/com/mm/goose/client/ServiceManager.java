package com.mm.goose.client;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mm.goose.netty.NettyRemotingClient;
import com.mm.goose.netty.RPCFuture;
import com.mm.goose.netty.config.Command;
import com.mm.goose.netty.config.ZookeeperConfiguration;
import com.mm.goose.netty.zookeeper.ZookeeperRegistryCenter;

/**
 * @author:chyl2005
 * @date:17/11/1
 * @time:17:23
 * @desc:描述该类的作用
 */
public class ServiceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceManager.class);

    private int defaultServerPort = 8383;

    private int maxTaskThread = 30;

    private int sessionTimeOut = 60000;

    private int zkConnectionTimeoutMs = 15000;

    private int maxRetryTimes = 3;

    private int maxSleepMs = 3000;

    private int baseSleepMs = 1000;

    private int detectInterval = 60;

    private String namespace = "goose";

    private int taskCallBackPort = 8383;

    private int acceptPort = 8410;

    //默认使用zk来发现服务节点
    private int useZkDetectNodes = 0;

    private NettyRemotingClient remotingClient = NettyRemotingClient.getInstance();


    /**
     * 服务节点
     */
    private Set<String> serverNodes = new HashSet<>();

    private ZookeeperRegistryCenter zookeeperRegistryCenter = new ZookeeperRegistryCenter();

    private static final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    @PostConstruct
    public void init() {
        ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration();
        zookeeperConfiguration.setBaseSleepTimeMilliseconds(this.getBaseSleepMs());
        zookeeperConfiguration.setConnectionTimeoutMilliseconds(this.getZkConnectionTimeoutMs());
        zookeeperConfiguration.setMaxRetries(this.getMaxRetryTimes());
        zookeeperConfiguration.setMaxSleepTimeMilliseconds(this.getMaxSleepMs());
        zookeeperConfiguration.setNamespace(this.getNamespace());
        zookeeperConfiguration.setSessionTimeoutMilliseconds(this.getSessionTimeOut());
        LOGGER.info("ServerNodeManager init zookeeper registry center begin.");
        zookeeperRegistryCenter.setZkConfig(zookeeperConfiguration);
        zookeeperRegistryCenter.init();

        CuratorFramework curatorFramework = zookeeperRegistryCenter.getRawClient();
        //节点监听
        curatorFramework.getConnectionStateListenable().addListener(new NodeStateListener(), executor);
        //缓存节点监听
        zookeeperRegistryCenter.getRawCache().getListenable().addListener(new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent event) throws Exception {
                String path = null == event.getData() ? "" : event.getData().getPath();
                try {
                    if (event.getType() == TreeCacheEvent.Type.NODE_ADDED) {
                        LOGGER.info("new server node join : {}", path);
                        serverNodes = getServerNodesDirectly();
                        remotingClient.updateServerNodes(serverNodes);
                    } else if (event.getType() == TreeCacheEvent.Type.NODE_REMOVED) {
                        LOGGER.info("server node : {} down.", path);
                        serverNodes = getServerNodesDirectly();
                        remotingClient.updateServerNodes(serverNodes);
                    }
                } catch (Exception e) {
                    LOGGER.error("TreeCacheListener capture data change and get data " + "failed.", e);
                }
            }
        });
        serverNodes = getServerNodesDirectly();
        remotingClient.updateServerNodes(serverNodes);
    }



    private Set<String> getServerNodesDirectly() {
        return zookeeperRegistryCenter.getServerNodesDirectly();
    }

    public int getDefaultServerPort() {
        return defaultServerPort;
    }

    public int getMaxTaskThread() {
        return maxTaskThread;
    }

    public int getSessionTimeOut() {
        return sessionTimeOut;
    }

    public int getZkConnectionTimeoutMs() {
        return zkConnectionTimeoutMs;
    }

    public int getMaxRetryTimes() {
        return maxRetryTimes;
    }

    public int getMaxSleepMs() {
        return maxSleepMs;
    }

    public int getBaseSleepMs() {
        return baseSleepMs;
    }

    public int getDetectInterval() {
        return detectInterval;
    }

    public String getNamespace() {
        return namespace;
    }

    public int getTaskCallBackPort() {
        return taskCallBackPort;
    }

    public int getAcceptPort() {
        return acceptPort;
    }

    public int getUseZkDetectNodes() {
        return useZkDetectNodes;
    }

    public ZookeeperRegistryCenter getZookeeperRegistryCenter() {
        return zookeeperRegistryCenter;
    }

}
