package org.openbase.rct.impl;

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
