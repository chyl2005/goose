package com.mm.goose.netty.zookeeper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.shaded.com.google.common.base.Preconditions;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.mm.goose.netty.config.ZookeeperConfiguration;
import com.mm.goose.netty.exception.RegExceptionHandler;

public class ZookeeperRegistryCenter implements CoordinatorRegistryCenter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperRegistryCenter.class);
    private static final String UTF_8 = "UTF-8";
    private static final String NAMESPACE = "/goose";
    private static final String NODES = NAMESPACE + "/nodes";
    private static final String EMPTY = "";
    private CuratorFramework client;
    @Autowired
    private ZookeeperConfiguration zkConfig;

    private TreeCache cache;

    /**
     * 初始化注册中心.
     */
    @Override
    public void init() {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory
                .builder().ensembleProvider(new DynamicEnsembleProvider())
                .retryPolicy(new RetryNTimes(zkConfig.getMaxRetries(), zkConfig.getMaxSleepTimeMilliseconds()));

        if (0 != zkConfig.getSessionTimeoutMilliseconds()) {
            builder.sessionTimeoutMs(zkConfig.getSessionTimeoutMilliseconds());
        }
        if (0 != zkConfig.getConnectionTimeoutMilliseconds()) {
            builder.connectionTimeoutMs(zkConfig.getConnectionTimeoutMilliseconds());
        }
        client = builder.build();
        client.start();
        try {
            client.blockUntilConnected();
            initZkNodes();
            cacheData();
        } catch (final Exception ex) {
            RegExceptionHandler.handleException(ex);
        }

    }

    private void initZkNodes() throws Exception {
        persist(NODES, EMPTY);
    }

    private void cacheData() throws Exception {
        cache = new TreeCache(client, NODES);
        cache.start();
    }

    /**
     * 直接获取服务端节点
     * @return
     */
    public Set<String> getServerNodesDirectly() {
        List<String> schedulers = getChildrenKeys(NODES);
        return schedulers.stream().collect(Collectors.toSet());
    }
    @Override
    @PreDestroy
    public void close() {
        try {
            if (null != cache) {
                cache.close();
            }
            waitForCacheClose();
            CloseableUtils.closeQuietly(client);
        } catch (Throwable t) {
            LOGGER.error("");
        }
    }

    /*
     * 因为异步处理, 可能会导致client先关闭而cache还未关闭结束.
     * 等待Curator新版本解决这个bug.
     * BUG地址：https://issues.apache.org/jira/browse/CURATOR-157
     */
    private void waitForCacheClose() {
        try {
            Thread.sleep(500L);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String get(final String key) {
        if (null == cache) {
            return null;
        }
        ChildData resultIncache = cache.getCurrentData(key);
        if (null != resultIncache) {
            return null == resultIncache.getData() ? null : new String(resultIncache.getData(), Charset.forName(UTF_8));
        }
        return null;
    }

    @Override
    public boolean isExisted(final String key) {

        try {
            return client.checkExists().forPath(key) != null;
        } catch (Exception k) {
            RegExceptionHandler.handleException(k);
        }
        return false;
    }

    @Override
    public void persist(final String key, final String value) {
        try {
            if (!isExisted(key)) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath
                        (key, value.getBytes(Charset.forName(UTF_8)));
            } else {
                update(key, value);
            }
        } catch (Exception x) {
            RegExceptionHandler.handleException(x);
        }

    }

    @Override
    public void update(final String key, final String value) {
        try {
            client.inTransaction().check().forPath(key).and().setData().forPath(key, value
                    .getBytes(Charset.forName(UTF_8))).and().commit();
        } catch (Exception k) {
            RegExceptionHandler.handleException(k);
        }
    }

    @Override
    public void remove(final String key) {
        try {
            client.delete().deletingChildrenIfNeeded().forPath(key);
        } catch (final Exception ex) {
            RegExceptionHandler.handleException(ex);
        }
    }

    @Override
    public long getRegistryCenterTime(final String key) {
        long result = 0L;
        try {
            String path = client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(key);
            result = client.checkExists().forPath(path).getCtime();
        } catch (final Exception ex) {
            RegExceptionHandler.handleException(ex);
        }
        Preconditions.checkState(0L != result, "Cannot get registry center time.");
        return result;
    }

    @Override
    public CuratorFramework getRawClient() {
        return client;
    }

    /**
     * 直接从注册中心而非本地缓存获取数据.
     *
     * @param key 键
     * @return 值
     */
    @Override
    public String getDirectly(String key) throws InterruptedException, IOException, KeeperException {
        return null;
    }


    @Override
    public List<String> getChildrenKeys(final String key) {
        List<String> values = new ArrayList<String>();
        try {
            values = client.getChildren().forPath(key);
        } catch (Exception k) {
            RegExceptionHandler.handleException(k);
        }
        return values;
    }

    @Override
    public void persistEphemeral(final String key, final String value) {
        try {
            if (isExisted(key)) {
                try {
                    client.delete().deletingChildrenIfNeeded().forPath(key);
                } catch (KeeperException.NoNodeException e) {
                    LOGGER.info("{} was always deleted", key);
                }
            }
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(key, value.getBytes(Charset.forName("UTF-8")));
        } catch (final Exception ex) {
            RegExceptionHandler.handleException(ex);
        }
    }

    @Override
    public void persistEphemeral(String key, String value, boolean overwrite) {
        try {
            if (overwrite) {
                persistEphemeral(key, value);
            } else {
                if (!isExisted(key)) {
                    client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(key, value.getBytes(Charset.forName("UTF-8")));
                }
            }
        } catch (final Exception ex) {
            RegExceptionHandler.handleException(ex);
        }
    }

    @Override
    public void persistEphemeralSequential(final String key) {
        try {
            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(key);
        } catch (final Exception ex) {
            RegExceptionHandler.handleException(ex);
        }
    }

    /**
     * 获取注册中心数据缓存对象.
     *
     * @return 注册中心数据缓存对象
     */
    @Override
    public TreeCache getRawCache() {
        return cache;
    }

    public ZookeeperConfiguration getZkConfig() {
        return zkConfig;
    }

    public void setZkConfig(ZookeeperConfiguration zkConfig) {
        this.zkConfig = zkConfig;
    }
}
