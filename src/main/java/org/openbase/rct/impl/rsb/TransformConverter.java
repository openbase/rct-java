package org.openbase.rct.impl.rsb;

/*-
 * #%L
 * RCT
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.nio.ByteBuffer;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.openbase.rct.Transform;
import org.openbase.type.geometry.FrameTransformType.FrameTransform;
import rsb.converter.ConversionException;
import rsb.converter.Converter;
import rsb.converter.ConverterSignature;
import rsb.converter.ProtocolBufferConverter;
import rsb.converter.UserData;
import rsb.converter.WireContents;
import org.openbase.type.geometry.RotationType.Rotation;
import org.openbase.type.geometry.TranslationType.Translation;
import org.openbase.type.timing.TimestampType.Timestamp;

public class TransformConverter implements Converter<ByteBuffer> {

    private final ProtocolBufferConverter<FrameTransform> converter = new ProtocolBufferConverter<>(FrameTransform.getDefaultInstance());

    private final ConverterSignature signature;

    public TransformConverter() {
        this.signature = new ConverterSignature(getWireSchema(), Transform.class);
    }

    private String getWireSchema() {
        return "." + FrameTransform.getDescriptor().getFullName();
    }

    @Override
    public WireContents<ByteBuffer> serialize(Class<?> typeInfo, Object obj) throws ConversionException {
        FrameTransform ft = convertTransformToPb((Transform) obj);
        return converter.serialize(typeInfo, ft);
    }

    @Override
    public UserData<Transform> deserialize(String wireSchema, ByteBuffer buffer) throws ConversionException {
        assert wireSchema.contentEquals(this.getWireSchema());

        Transform result = convertPbToTransform((FrameTransform) converter.deserialize(wireSchema, buffer).getData());
        return new UserData<>(result, result.getClass());
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

        Transform newTrans = new Transform(transform3d, t.getFrameParent(), t.getFrameChild(), timeMSec);
        return newTrans;
    }

    @Override
    public ConverterSignature getSignature() {
        return signature;
    }
}
