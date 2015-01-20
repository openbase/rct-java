package rct.impl;

import javax.xml.transform.TransformerException;

public interface TransformCache {

	public class TimeAndFrameID {
		public TimeAndFrameID(long time, int frameID) {
			this.time = time;
			this.frameID = frameID;
		}

		public long time;
		public int frameID;
	}

	boolean getData(long time, TransformInternal data_out); // returns false if
															// data unavailable
															// (should be thrown
															// as lookup
															// exception

	/** \brief Insert data into the cache */
	boolean insertData(TransformInternal new_data);

	/** @brief Clear the list of stored values */
	void clearList();

	/** \brief Retrieve the parent at a specific time */
	int getParent(long time);

	/**
	 * \brief Get the latest time stored in this cache, and the parent
	 * associated with it. Returns parent = 0 if no data.
	 */
	TimeAndFrameID getLatestTimeAndParent();

	// / Debugging information methods
	/** @brief Get the length of the stored list */
	int getListLength();

	/** @brief Get the latest timestamp cached */
	long getLatestTimestamp();

	/** @brief Get the oldest timestamp cached */
	long getOldestTimestamp();
	
	boolean isValid();
}
