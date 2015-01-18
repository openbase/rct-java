package rct.impl;

public class TransformCacheStatic implements TransformCache {

	private TransformInternal storage = new TransformInternal();

	public TransformCacheStatic() {
	}

	public boolean getData(long time, TransformInternal data_out) {
		data_out = storage;
		data_out.stamp = time;
		return true;
	}

	public boolean insertData(TransformInternal new_data) {
		storage = new_data;
		return true;
	}

	public void clearList() {
		return;
	}

	public int getParent(long time) {
		return storage.frame_id;
	}

	public TimeAndFrameID getLatestTimeAndParent() {
		return new TimeAndFrameID(0, storage.frame_id);
	}

	public int getListLength() {
		return 1;
	}

	public long getLatestTimestamp() {
		return 0;
	}

	public long getOldestTimestamp() {
		return 0;
	}

	public boolean isValid() {
		return true;
	}
}
