package org.openbase.rct.impl;

public class TransformCacheNull implements TransformCache {

    @Override
    public boolean getData(long time, TransformInternal data_out) {
        return false;
    }

    @Override
    public boolean insertData(TransformInternal new_data) {
        return false;
    }

    @Override
    public void clearList() {
    }

    @Override
    public int getParent(long time) {
        return -1;
    }

    @Override
    public TimeAndFrameID getLatestTimeAndParent() {
        return null;
    }

    @Override
    public int getListLength() {
        return 0;
    }

    @Override
    public long getLatestTimestamp() {
        return 0;
    }

    @Override
    public long getOldestTimestamp() {
        return 0;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public String toString() {
        return "TransformCacheNull[invalid]";
    }
}
