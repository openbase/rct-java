package rct.impl;

public class TransformCacheNull implements TransformCache {

	public boolean getData(long time, TransformInternal data_out) {
		return false;
	}

	public boolean insertData(TransformInternal new_data) {
		return false;
	}

	public void clearList() {
	}

	public int getParent(long time) {
		return -1;
	}

	public TimeAndFrameID getLatestTimeAndParent() {
		return null;
	}

	public int getListLength() {
		return 0;
	}

	public long getLatestTimestamp() {
		return 0;
	}

	public long getOldestTimestamp() {
		return 0;
	}

	public boolean isValid() {
		return false;
	}
	
	@Override
	public String toString() {
		return "TransformCacheNull[invalid]";
	}
}
