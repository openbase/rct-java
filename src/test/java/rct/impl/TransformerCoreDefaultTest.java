package rct.impl;

import static org.junit.Assert.*;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Test;

import rct.Transform;
import rct.TransformerException;

public class TransformerCoreDefaultTest {

	private Logger logger = Logger.getLogger(TransformerCoreDefaultTest.class);
	
	public TransformerCoreDefaultTest() {
		BasicConfigurator.configure();
	}
	@Test
	public void testSetTransform() throws TransformerException {

		Quat4d q = new Quat4d(0, 1, 2, 1);
		Vector3d v = new Vector3d(0, 1, 2);
		Transform3D t = new Transform3D(q,v,1);
		Transform transform = new Transform(t,"foo", "bar", System.currentTimeMillis());
		transform.setAuthority(TransformerCoreDefaultTest.class.getSimpleName());

		TransformerCoreDefault core = new TransformerCoreDefault(1000);
		
		core.setTransform(transform, false);
		
		String framesAsString = core.allFramesAsString();
		
		logger.debug("framesAsString: " + framesAsString);
		
		assertTrue(framesAsString.contains("foo"));
		assertTrue(framesAsString.contains("bar"));
	}

}
