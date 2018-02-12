package com.mm.goose.client;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.Set;
import com.mm.goose.netty.zookeeper.ZookeeperRegistryCenter;

/**
 * @author:chyl2005
 * @date:17/11/1
 * @time:20:05
 * @desc:描述该类的作用
 */
public class ServiceSelector {

    private ZookeeperRegistryCenter zookeeperRegistryCenter;
    private Set<String> serverNodes;

    private final Random random = new Random();

    public ServiceSelector(ZookeeperRegistryCenter zookeeperRegistryCenter) {
        this.zookeeperRegistryCenter = zookeeperRegistryCenter;
        this.serverNodes = zookeeperRegistryCenter.getServerNodesDirectly();
    }


    public InetSocketAddress select(){

        return null;
    }
}
