package org.openbase.rct;

public class TransformerConfig {

    public enum CommunicatorType {
        AUTO, RSB, ROS
    };

    private long cacheTime = 30000;
    private CommunicatorType commType = CommunicatorType.AUTO;

    public TransformerConfig() {
    }

    public TransformerConfig(long cacheTime) {
        this.cacheTime = cacheTime;
    }

    public TransformerConfig(CommunicatorType commType, long cacheTime) {
        this.cacheTime = cacheTime;
        this.commType = commType;
    }

    public long getCacheTime() {
        return cacheTime;
    }

    public void setCacheTime(long cacheTime) {
        this.cacheTime = cacheTime;
    }

    public CommunicatorType getCommType() {
        return commType;
    }

    public void setCommType(CommunicatorType commType) {
        this.commType = commType;
    }
}
