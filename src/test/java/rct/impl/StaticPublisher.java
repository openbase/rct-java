package rct.impl;

import java.io.IOException;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.exit;
import static java.lang.System.in;
import static java.lang.System.out;
import static java.lang.Thread.sleep;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;

import rct.Transform;
import rct.TransformType;
import rct.TransformPublisher;
import static rct.TransformType.STATIC;
import rct.TransformerException;
import rct.TransformerFactory;
import rct.TransformerFactory.TransformerFactoryException;
import static rct.TransformerFactory.getInstance;

public class StaticPublisher {

	private static TransformPublisher transformer;

	public static void main(String[] args) {

		try {
			transformer = getInstance().createTransformPublisher("static-publisher-java");


			Transform3D transform = new Transform3D(new Quat4f(1, 0, 0, 1), new Vector3d(1, 2, 3), 1.0);
			Transform t = new Transform(transform, "start", "foo", currentTimeMillis());

			transformer.sendTransform(t, STATIC);

			sleep(1000);
			out.println("Press ENTER to exit");
			in.read();

		} catch (TransformerException | TransformerFactoryException | IOException | InterruptedException e) {
			e.printStackTrace();
			exit(1);
		}

		out.println("done");
		exit(0);
	}
	}
