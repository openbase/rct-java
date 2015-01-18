package rct.impl;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class TransformCacheImpl implements TransformCache {

	private Logger logger = Logger.getLogger(TransformCacheImpl.class);
	private long maxStorageTime;
	private List<TransformInternal> storage_ = new LinkedList<TransformInternal>();
	
	public TransformCacheImpl(long maxStorageTime) {
		this.maxStorageTime = maxStorageTime;
	}

	int findClosest(TransformInternal one, TransformInternal two, long target_time) {
		// No values stored
		if (storage_.isEmpty()) {
			logger.debug("findClosest() storage is empty");
			return 0;
		}

		// If time == 0 return the latest
		if (target_time == 0) {
			one.replaceWith(storage_.get(0));
			logger.debug("findClosest() time is zero. Return latest.");
			return 1;
		}

		// One value stored
		if (storage_.size() == 1) {
			TransformInternal ts = storage_.get(0);
			if (ts.stamp == target_time) {
				one.replaceWith(ts);
				logger.debug("findClosest() storage has only one entry. Return it.");
				return 1;
			} else {
				throw new RuntimeException(
						"Lookup would require extrapolation at time "
								+ target_time + ", but only time " + ts.stamp
								+ " is in the buffer");
			}
		}

		long latest_time = storage_.get(0).stamp;
		long earliest_time = storage_.get(storage_.size() - 1).stamp;

		if (target_time == latest_time) {
			one.replaceWith(storage_.get(0));
			logger.debug("findClosest() found exact target time. Return it.");
			return 1;
		} else if (target_time == earliest_time) {
			one.replaceWith(storage_.get(storage_.size() - 1));
			logger.debug("findClosest() found exact target time. Return it.");
			return 1;
		}
		// Catch cases that would require extrapolation
		else if (target_time > latest_time) {
			throw new RuntimeException(
					"Lookup would require extrapolation into the future.  Requested time "
							+ target_time + " but the latest data is at time "
							+ latest_time);
		} else if (target_time < earliest_time) {
			throw new RuntimeException(
					"Lookup would require extrapolation into the future.  Requested time "
							+ target_time + " but the latest data is at time "
							+ earliest_time);
		}

		// At least 2 values stored
		// Find the first value less than the target value
		int storage_it = -1;
		for (int i = 0; i < storage_.size(); i++) {
			if (storage_.get(i).stamp <= target_time) {
				storage_it = i;
				break;
			}
		}

		// Finally the case were somewhere in the middle Guarenteed no
		// extrapolation :-)
		logger.debug("findClosest() return the two closest.");
		one.replaceWith(storage_.get(storage_it)); // Older
		two.replaceWith(storage_.get(storage_it - 1)); // Newer
		return 2;

	}

	void interpolate(TransformInternal one, TransformInternal two, long time,
			TransformInternal output) {
		// Check for zero distance case
		if (two.stamp == one.stamp) {
			output.replaceWith(two);
			return;
		}
		// Calculate the ratio
		double ratio = (double) (time - one.stamp)
				/ (double) (two.stamp - one.stamp);

		// Interpolate translation
		output.translation.interpolate(one.translation, two.translation, ratio);

		// Interpolate rotation
		output.rotation.interpolate(one.rotation, two.rotation, ratio);

		output.stamp = one.stamp;
		output.frame_id = one.frame_id;
		output.child_frame_id = one.child_frame_id;
	}

	public boolean getData(long time, TransformInternal data_out) {
		TransformInternal p_temp_1 = new TransformInternal();
		TransformInternal p_temp_2 = new TransformInternal();

		logger.debug("getData() find closest to time " + time);
		int num_nodes = findClosest(p_temp_1, p_temp_2, time);
		logger.debug("getData() nodes: " + num_nodes);
		if (num_nodes == 0) {
			logger.error("getData() no transform found");
			return false;
		} else if (num_nodes == 1) {
			logger.debug("getData() found exactly one transform");
			data_out.replaceWith(p_temp_1);
		} else if (num_nodes == 2) {
			if (p_temp_1.frame_id == p_temp_2.frame_id) {
				logger.debug("getData() found two transforms. Interpolate.");
				interpolate(p_temp_1, p_temp_2, time, data_out);
			} else {
				data_out.replaceWith(p_temp_1);
			}
		} else {
			assert (false);
		}

		return true;
	}

	public boolean insertData(TransformInternal new_data) {
		logger.debug("insertData(): " + new_data);
		int storage_it = 0;

		if (!storage_.isEmpty()) {
			logger.debug("storage is not empty");
			if (storage_.get(storage_it).stamp > new_data.stamp
					+ maxStorageTime) {
				logger.error("data too old for insertion");
				return false;
			}
		}

		while (storage_it != storage_.size()) {
			if (storage_.get(storage_it).stamp <= new_data.stamp)
				break;
			storage_it++;
		}
		if (storage_it >= storage_.size()) {
			logger.debug("add additional storage entry (storage size:" + storage_it + ")");
			storage_.add(new_data);
		} else {
			logger.debug("set new data to index " + storage_it + " in storage (size:" + storage_.size()+ ")");
			storage_.set(storage_it, new_data);
		}

		logger.debug("prune list");
		pruneList();
		return true;
	}

	private void pruneList() {
		long latest_time = storage_.get(0).stamp;
		logger.debug("latest time: " + latest_time);
		logger.debug("max storage time: " + maxStorageTime);
		logger.debug("storage empty: " + storage_.isEmpty());
		while (!storage_.isEmpty()
				&& storage_.get(storage_.size() - 1).stamp + maxStorageTime < latest_time) {
			logger.debug("remove last. stamp: " + storage_.get(storage_.size() - 1).stamp);
			storage_.remove(storage_.size() - 1);
		}
	}

	public void clearList() {
		storage_.clear();
	}

	public int getParent(long time) {
		TransformInternal p_temp_1 = null;
		TransformInternal p_temp_2 = null;

		int num_nodes = findClosest(p_temp_1, p_temp_2, time);
		if (num_nodes == 0) {
			return 0;
		}

		return p_temp_1.frame_id;
	}

	public TimeAndFrameID getLatestTimeAndParent() {
		if (storage_.isEmpty()) {
			return new TimeAndFrameID(0, 0);
		}

		TransformInternal ts = storage_.get(0);
		return new TimeAndFrameID(ts.stamp, ts.frame_id);
	}

	public int getListLength() {
		return storage_.size();
	}

	public long getLatestTimestamp() {
		if (storage_.isEmpty())
			return 0l; // empty list case
		return storage_.get(0).stamp;
	}

	public long getOldestTimestamp() {
		if (storage_.isEmpty())
			return 0l; // empty list case
		return storage_.get(storage_.size() - 1).stamp;
	}

	public boolean isValid() {
		return true;
	}

	@Override
	public String toString() {
		return "TransformCacheImpl[maxStorageTime:" + maxStorageTime + ", storage:" + storage_.size() + "]";
	}
}
