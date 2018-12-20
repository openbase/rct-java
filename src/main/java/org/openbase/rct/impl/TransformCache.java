package org.openbase.rct.impl;

public interface TransformCache {

    public class TimeAndFrameID {

        public TimeAndFrameID(long time, int frameID) {
            this.time = time;
            this.frameID = frameID;
        }

        public long time;
        public int frameID;
    }

    boolean getData(long time, TransformInternal dataOut); // returns false if
    // data unavailable
    // (should be thrown
    // as lookup
    // exception

    /**
     * \brief Insert data into the cache
     *
     * @param newData
     * @return
     */
    boolean insertData(TransformInternal newData);

    /**
     * @brief Clear the list of stored values
     */
    void clearList();

    /**
     * \brief Retrieve the parent at a specific time
     *
     * @param time
     * @return
     */
    int getParent(long time);

    /**
     * \brief Get the latest time stored in this cache, and the parent
     * associated with it. Returns parent = 0 if no data.
     *
     * @return
     */
    TimeAndFrameID getLatestTimeAndParent();

    // / Debugging information methods
    /**
     * @return * @brief Get the length of the stored list
     */
    int getListLength();

    /**
     * @return * @brief Get the latest timestamp cached
     */
    long getLatestTimestamp();

    /**
     * @return * @brief Get the oldest timestamp cached
     */
    long getOldestTimestamp();

    boolean isValid();
}
