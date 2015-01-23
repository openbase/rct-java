package rct;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;

public class Transform {

	private Transform3D transform;
	private String frameParent;
	private String frameChild;
	private long time;
	private String authority;

	public Transform(Transform transform) {
		this.transform = new Transform3D(transform.transform);
		this.frameParent = new String(transform.frameParent);
		this.frameChild = new String(transform.frameChild);
		this.time = transform.time;
		this.authority = transform.authority;
	}

	public Transform(Transform3D transform, String frameParent,
			String frameChild, long time) {
		this.transform = transform;
		this.frameParent = frameParent;
		this.frameChild = frameChild;
		this.time = time;
	}

	public Transform3D getTransform() {
		return transform;
	}

	public Vector3d getTranslation() {
		Vector3d translation = new Vector3d();
		transform.get(translation);
		return translation;
	}

	public Quat4d getRotationQuat() {
		Quat4d quat = new Quat4d();
		transform.get(quat);
		return quat;
	}

	public Matrix3d getRotationMatrix() {
		Matrix3d rot = new Matrix3d();
		transform.get(rot);
		return rot;
	}

	public Vector3d getRotationYPR() {

		Matrix3d rot = getRotationMatrix();

		// this code is taken from buttel btMatrix3x3 getEulerYPR().
		// http://bulletphysics.org/Bullet/BulletFull/btMatrix3x3_8h_source.html
		// first use the normal calculus
		double yawOut = Math.atan2(rot.m10, rot.m00);
		double pitchOut = Math.asin(-rot.m20);
		double rollOut = Math.atan2(rot.m21, rot.m22);

		// on pitch = +/-HalfPI
		if (Math.abs(pitchOut) == Math.PI / 2.0) {
			if (yawOut > 0)
				yawOut -= Math.PI;
			else
				yawOut += Math.PI;
			if (pitchOut > 0)
				pitchOut -= Math.PI;
			else
				pitchOut += Math.PI;
		}

		return new Vector3d(yawOut, pitchOut, rollOut);
	}

	public void setTransform(Transform3D transform) {
		this.transform = transform;
	}

	public String getFrameParent() {
		return frameParent;
	}

	public void setFrameParent(String frameParent) {
		this.frameParent = frameParent;
	}

	public String getFrameChild() {
		return frameChild;
	}

	public void setFrameChild(String frameChild) {
		this.frameChild = frameChild;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public String getAuthority() {
		return authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}

	@Override
	public String toString() {

		Matrix4d mat = new Matrix4d();
		transform.get(mat);
		String tStr = String
				.format("{%.2f %.2f %.2f %.2f; %.2f %.2f %.2f %.2f; %.2f %.2f %.2f %.2f; %.2f %.2f %.2f %.2f}",
						mat.m00, mat.m01, mat.m02, mat.m03, mat.m10, mat.m11,
						mat.m12, mat.m13, mat.m20, mat.m21, mat.m22, mat.m23,
						mat.m30, mat.m31, mat.m32, mat.m33);

		return "Transform[parent:" + frameParent + "; child:" + frameChild
				+ "; time:" + time + "; transform:" + tStr + "]";
	}
}
