package org.openbase.rct.type;

/*
 * #%L
 * JUL Extension RCT
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

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import org.openbase.rct.Transform;
import org.openbase.type.geometry.PoseType;
import org.openbase.type.geometry.RotationType;
import org.openbase.type.geometry.TranslationType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PoseTransformer {

    public static Transform transform(final PoseType.Pose position, String frameParent, String frameChild) {
        return new Transform(transform(position), frameParent, frameChild, System.currentTimeMillis());
    }

    public static Transform3D transform(final PoseType.Pose position) {
        RotationType.Rotation pRotation = position.getRotation();
        TranslationType.Translation pTranslation = position.getTranslation();
        Quat4d jRotation = new Quat4d(pRotation.getQx(), pRotation.getQy(), pRotation.getQz(), pRotation.getQw());
        Vector3d jTranslation = new Vector3d(pTranslation.getX(), pTranslation.getY(), pTranslation.getZ());
        return new Transform3D(jRotation, jTranslation, 1.0);
    }
}
