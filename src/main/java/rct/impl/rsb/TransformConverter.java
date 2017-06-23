package rct.impl.rsb;

import java.nio.ByteBuffer;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import rct.Transform;
import rct.proto.FrameTransformType.FrameTransform;
import rsb.converter.ConversionException;
import rsb.converter.Converter;
import rsb.converter.ConverterSignature;
import rsb.converter.ProtocolBufferConverter;
import rsb.converter.UserData;
import rsb.converter.WireContents;
import rst.geometry.RotationType.Rotation;
import rst.geometry.TranslationType.Translation;
import rst.timing.TimestampType.Timestamp;

public class TransformConverter implements Converter<ByteBuffer> {

	private ProtocolBufferConverter<FrameTransform> converter = new ProtocolBufferConverter<>(
			FrameTransform.getDefaultInstance());

	private final ConverterSignature signature;

	public TransformConverter() {
		this.signature = new ConverterSignature(getWireSchema(), Transform.class);
	}

	private String getWireSchema() {
		return "." + FrameTransform.getDescriptor().getFullName();
	}

	@Override
	public WireContents<ByteBuffer> serialize(Class<?> typeInfo, Object obj)
			throws ConversionException {
		FrameTransform ft = convertTransformToPb((Transform)obj);
		return converter.serialize(typeInfo, ft);
	}

	@Override
	public UserData<Transform> deserialize(String wireSchema, ByteBuffer buffer)
			throws ConversionException {
		assert wireSchema.contentEquals(this.getWireSchema());
		
		Transform result = convertPbToTransform((FrameTransform)converter.deserialize(wireSchema, buffer).getData());
        return new UserData<Transform>(result, result.getClass());
	}

	private FrameTransform convertTransformToPb(Transform t) {

		long timeMSec = t.getTime();
		long timeUSec = timeMSec * 1000l;

		Quat4d quat = t.getRotationQuat();
		Vector3d vec = t.getTranslation();

		FrameTransform.Builder tBuilder = FrameTransform.newBuilder();
		tBuilder.getTimeBuilder().setTime(timeUSec);
		tBuilder.setFrameChild(t.getFrameChild());
		tBuilder.setFrameParent(t.getFrameParent());
		tBuilder.getTransformBuilder().getRotationBuilder().setQw(quat.w);
		tBuilder.getTransformBuilder().getRotationBuilder().setQx(quat.x);
		tBuilder.getTransformBuilder().getRotationBuilder().setQy(quat.y);
		tBuilder.getTransformBuilder().getRotationBuilder().setQz(quat.z);
		tBuilder.getTransformBuilder().getTranslationBuilder().setX(vec.x);
		tBuilder.getTransformBuilder().getTranslationBuilder().setY(vec.y);
		tBuilder.getTransformBuilder().getTranslationBuilder().setZ(vec.z);

		return tBuilder.build();
	}

	private Transform convertPbToTransform(FrameTransform t) {

		Timestamp time = t.getTime();
		long timeUSec = time.getTime();
		long timeMSec = timeUSec / 1000l;

		Rotation rstRot = t.getTransform().getRotation();
		Translation rstTrans = t.getTransform().getTranslation();

		Quat4d quat = new Quat4d(rstRot.getQx(), rstRot.getQy(), rstRot.getQz(), rstRot.getQw());
		Vector3d vec = new Vector3d(rstTrans.getX(), rstTrans.getY(), rstTrans.getZ());

		Transform3D transform3d = new Transform3D(quat, vec, 1.0);

		Transform newTrans = new Transform(transform3d, t.getFrameParent(), t.getFrameChild(),
				timeMSec);
		return newTrans;
	}

	@Override
	public ConverterSignature getSignature() {
		return signature;
	}
}
