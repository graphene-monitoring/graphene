package com.graphene.reader.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Andrei Ivanov
 */
public class StatsConfiguration {
    private static final boolean DEFAULT_ENABLED = false;

    private boolean enabled = DEFAULT_ENABLED;
    private int interval;
    private String tenant;
    private String hostname;
    private String carbonHost;
    private int carbonPort;

    public StatsConfiguration() {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getCarbonHost() {
        return carbonHost;
    }

    public void setCarbonHost(String carbonHost) {
        this.carbonHost = carbonHost;
    }

    public int getCarbonPort() {
        return carbonPort;
    }

    public void setCarbonPort(int carbonPort) {
        this.carbonPort = carbonPort;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "StatsConfiguration{" +
                "interval=" + interval +
                ", tenant='" + tenant + '\'' +
                ", hostname='" + hostname + '\'' +
                ", carbonHost='" + carbonHost + '\'' +
                ", carbonPort=" + carbonPort +
                ", enabled=" + enabled +
                '}';
    }
}
