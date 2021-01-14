package org.openbase.rct.impl;

/*-
 * #%L
 * RCT
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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

import java.io.IOException;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;

import org.openbase.rct.TransformerFactory.TransformerFactoryException;
import org.openbase.rct.Transform;
import org.openbase.rct.TransformType;
import org.openbase.rct.TransformPublisher;
import org.openbase.rct.TransformerException;
import org.openbase.rct.TransformerFactory;

public class StaticPublisher {

    private static TransformPublisher transformer;

    public static void main(String[] args) {

        try {
            transformer = TransformerFactory.getInstance().createTransformPublisher("static-publisher-java");

            Transform3D transform = new Transform3D(new Quat4f(1, 0, 0, 1), new Vector3d(1, 2, 3), 1.0);
            Transform t = new Transform(transform, "start", "foo", System.currentTimeMillis());

            transformer.sendTransform(t, TransformType.STATIC);

            Thread.sleep(1000);
            System.out.println("Press ENTER to exit");
            System.in.read();

        } catch (TransformerException | TransformerFactoryException | IOException | InterruptedException ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        System.out.println("done");
        System.exit(0);
    }
}
