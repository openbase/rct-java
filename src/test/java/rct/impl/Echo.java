package rct.impl;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.err;
import static java.lang.System.exit;
import static java.lang.System.out;
import static java.lang.Thread.sleep;
import rct.Transform;
import rct.TransformReceiver;
import rct.TransformerFactory;
import static rct.TransformerFactory.getInstance;

public class Echo {

		public static void main(String[] args) {

		if (args.length != 2) {
			err.println("Required 2 arguments!");
			exit(1);
		}
		try {
			TransformReceiver transformer = getInstance()
					.createTransformReceiver();

			sleep(1000);

			Transform t = transformer.lookupTransform(args[0], args[1],
					currentTimeMillis());

			out.println(t);
		} catch (Exception e) {
			e.printStackTrace();
			exit(1);
		}

		exit(0);
	}

}
