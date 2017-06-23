package rct.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformCacheStatic implements TransformCache {

    private TransformInternal storage = new TransformInternal();
    private static final Logger LOGGER = LoggerFactory.getLogger(TransformCacheImpl.class);

    public TransformCacheStatic() {
    }

    @Override
    public boolean getData(long time, TransformInternal dataOut) {
        dataOut.replaceWith(storage);
        dataOut.stamp = time;
        return true;
    }

    @Override
    public boolean insertData(TransformInternal newData) {
        LOGGER.debug("insertData(): " + newData);
        storage = newData;
        return true;
    }

    @Override
    public void clearList() {
    }

    @Override
    public int getParent(long time) {
        return storage.frame_id;
    }

    @Override
    public TimeAndFrameID getLatestTimeAndParent() {
        return new TimeAndFrameID(0, storage.frame_id);
    }

    @Override
    public int getListLength() {
        return 1;
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
        return true;
    }
}
