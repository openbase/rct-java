package rct.impl;

import java.util.Set;
import java.util.concurrent.Future;

import rct.Transform;

public class TransformerCoreDefault implements TransformerCore {
	
	public TransformerCoreDefault(long cacheTime) {
		// TODO Auto-generated constructor stub
	}

	public void clear() {
		// TODO Auto-generated method stub

	}

	public boolean setTransform(Transform transform, boolean is_static) {
		// TODO Auto-generated method stub
		return false;
	}

	public Transform lookupTransform(String target_frame, String source_frame,
			long time) {
		// TODO Auto-generated method stub
		return null;
	}

	public Transform lookupTransform(String target_frame, long target_time,
			String source_frame, long source_time, String fixed_frame) {
		// TODO Auto-generated method stub
		return null;
	}

	public Future<Transform> requestTransform(String target_frame,
			String source_frame, long time) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean canTransform(String target_frame, String source_frame,
			long time) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean canTransform(String target_frame, long target_time,
			String source_frame, long source_time, String fixed_frame) {
		// TODO Auto-generated method stub
		return false;
	}

	public Set<String> getFrameStrings() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean frameExists(String frame_id_str) {
		// TODO Auto-generated method stub
		return false;
	}

	public String getParent(String frame_id, long time) {
		// TODO Auto-generated method stub
		return null;
	}

	public String allFramesAsDot() {
		// TODO Auto-generated method stub
		return null;
	}

	public String allFramesAsYAML() {
		// TODO Auto-generated method stub
		return null;
	}

	public String allFramesAsString() {
		// TODO Auto-generated method stub
		return null;
	}

}
