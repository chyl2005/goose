package com.mm.goose.client;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author:chyl2005
 * Date:17/10/23
 * Time:16:13
 * Desc:描述该类的作用
 */
public class NodeStateListener implements ConnectionStateListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeStateListener.class);
    /**
     * Called when there is a state change in the connection
     *
     * @param client   the client
     * @param connectionState the new state
     */
    @Override
    public void stateChanged(CuratorFramework client, ConnectionState connectionState) {

        if (connectionState == ConnectionState.LOST) {
            LOGGER.warn("Network is unreachable to ZK with a long time, connection state LOST");
        } else if (connectionState == ConnectionState.SUSPENDED) {
            LOGGER.warn("Network is unreachable to ZK. Reconnection.....");
        } else if (connectionState == ConnectionState.RECONNECTED) {
            LOGGER.info("Network reachable to ZK,Reconnected to ZK.");

        }
    }
}
