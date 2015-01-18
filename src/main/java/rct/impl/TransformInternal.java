package rct.impl;

import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import rct.Transform;

public class TransformInternal {
	public TransformInternal() {

	}

	public TransformInternal(Vector3d translation, Quat4d rotation, int frameNumber,
			int childFrameNumber) {
		this.translation = translation;
		this.rotation = rotation;
		this.frame_id = frameNumber;
		this.child_frame_id = childFrameNumber;
	}
	
	public TransformInternal(Transform t, int frameNumber,
			int childFrameNumber) {
		this.translation = t.getTranslation();
		this.rotation = t.getRotationQuat();
		this.frame_id = frameNumber;
		this.child_frame_id = childFrameNumber;
	}

	TransformInternal(TransformInternal rhs) {
		this.translation = rhs.translation;
		this.rotation = rhs.rotation;
		this.frame_id = rhs.frame_id;
		this.child_frame_id = rhs.child_frame_id;
		this.stamp = rhs.stamp;
	}

	public String toString() {
		return "TransformInternal[parent:" + frame_id + ",child:" + child_frame_id + ",stamp:" + stamp  + ",t:" + translation + ",r:" + rotation + "]";
	};
	
	public void replaceWith(TransformInternal rhs) {
		this.translation = rhs.translation;
		this.rotation = rhs.rotation;
		this.frame_id = rhs.frame_id;
		this.child_frame_id = rhs.child_frame_id;
		this.stamp = rhs.stamp;
	}
	
	Vector3d translation;
	Quat4d rotation;
	long stamp;
	int frame_id;
	int child_frame_id;
}
