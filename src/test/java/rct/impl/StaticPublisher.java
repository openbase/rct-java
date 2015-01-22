package rct.impl;

import java.io.Console;
import java.io.IOException;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;

import org.apache.log4j.BasicConfigurator;

import rct.Transform;
import rct.Transformer;
import rct.TransformerException;
import rct.TransformerFactory;
import rct.TransformerFactory.TransformerFactoryException;

public class StaticPublisher {
	
	private static Transformer transformer;

	public static void main(String[] args) {
		
		BasicConfigurator.configure();
		
		try {
			transformer = TransformerFactory.getInstance().createTransformer();
		
		
			Transform3D transform = new Transform3D(new Quat4f(1, 0, 0, 1), new Vector3d(1, 2, 3), 1.0);
			Transform t = new Transform(transform, "start", "foo", System.currentTimeMillis());

			transformer.sendTransform(t, true);
			
			System.out.println("Press ENTER to exit");
			System.in.read();
			
		} catch (TransformerException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (TransformerFactoryException e1) {
			e1.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("done");
	    
	    System.exit(0);
	}

}
