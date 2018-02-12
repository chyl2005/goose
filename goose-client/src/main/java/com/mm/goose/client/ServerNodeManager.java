package com.mm.goose.client;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import com.mm.goose.client.utils.IPUtil;
import com.mm.goose.netty.config.ZookeeperConfiguration;
import com.mm.goose.netty.zookeeper.ZookeeperRegistryCenter;

/**
 * @author:chyl2005
 * @date:17/10/23
 * @time:19:59
 * 服务器节点管理
 */
public class ServerNodeManager implements ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContextAware.class);

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

    private ApplicationContext applicationContext;

    /**
     * 服务节点
     */
    private  Set<String> serverNodes=new HashSet<>();

    private ZookeeperRegistryCenter zookeeperRegistryCenter = new ZookeeperRegistryCenter();
    private static final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    /**
     * 服务端节点初始化
     */
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

        CuratorFramework curatorFramework =zookeeperRegistryCenter.getRawClient();
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
                        Set<String> nodes = getServerNodesDirectly();
                        resetServerNodes(nodes);
                    } else if (event.getType() == TreeCacheEvent.Type.NODE_REMOVED) {
                        LOGGER.info("server node : {} down.", path);
                        Set<String> nodes = getServerNodesDirectly();
                        resetServerNodes(nodes);
                    }
                } catch (Exception e) {
                    LOGGER.error("TreeCacheListener capture data change and get data " + "failed.", e);
                }
            }
        });

        Set<String> nodes = getServerNodesDirectly();
        resetServerNodes(nodes);
        registry();
    }
    public void registry() {

        String address = StringUtils.defaultIfEmpty(IPUtil.getAddress(), IPUtil.getLocalHost());
        String addressAndPort = address + ":" + acceptPort;
        zookeeperRegistryCenter.persistEphemeral(addressAndPort, addressAndPort);
        LOGGER.info("server Node : {} registry to ZK successfully.", address);
    }

    private Set<String> getServerNodesDirectly() {
        return zookeeperRegistryCenter.getServerNodesDirectly();
    }

    private void resetServerNodes(Set<String> nodes) {
        serverNodes.clear();
        serverNodes.addAll(nodes);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

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

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public ZookeeperRegistryCenter getZookeeperRegistryCenter() {
        return zookeeperRegistryCenter;
    }
}
