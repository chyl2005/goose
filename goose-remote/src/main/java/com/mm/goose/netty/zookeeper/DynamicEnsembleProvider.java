package com.mm.goose.netty.zookeeper;

import java.io.IOException;
import org.apache.curator.ensemble.EnsembleProvider;
import org.springframework.beans.factory.InitializingBean;


public class DynamicEnsembleProvider implements EnsembleProvider, InitializingBean {

    private String connectionString;

    @Override
    public void start() throws Exception {

    }

    @Override
    public String getConnectionString() {
        return connectionString;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void setConnectionString(String s) {

    }

    @Override
    public boolean updateServerListEnabled() {
        return false;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        connectionString = "127.0.0.1:2181";
    }
}
