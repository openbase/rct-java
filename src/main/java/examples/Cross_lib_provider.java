package examples;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.exit;
import static java.lang.System.out;
import static java.lang.Thread.sleep;
import java.util.logging.Level;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4d;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import org.slf4j.LoggerFactory;
import static org.slf4j.LoggerFactory.getLogger;
import rct.Transform;
import rct.TransformPublisher;
import rct.TransformReceiver;
import rct.TransformType;
import static rct.TransformType.DYNAMIC;
import static rct.TransformType.STATIC;
import rct.TransformerException;
import rct.TransformerFactory;
import static rct.TransformerFactory.getInstance;

/**
 *
 * @author nkoester
 */
public class Cross_lib_provider {

    private static final org.slf4j.Logger logger = getLogger(Cross_lib_provider.class);

    static TransformPublisher publisher;
    TransformReceiver receiver;

    public Cross_lib_provider() {

    }

    private Quat4f yrp2q(float roll, float pitch, float yaw) {

        float halfYaw = yaw * 0.5f;
        float halfPitch = pitch * 0.5f;
        float halfRoll = roll * 0.5f;

        float cosYaw = (float) cos(halfYaw);
        float sinYaw = (float) sin(halfYaw);
        float cosPitch = (float) cos(halfPitch);
        float sinPitch = (float) sin(halfPitch);
        float cosRoll = (float) cos(halfRoll);
        float sinRoll = (float) sin(halfRoll);

        return new Quat4f(sinRoll * cosPitch * cosYaw - cosRoll * sinPitch * sinYaw, //x
                cosRoll * sinPitch * cosYaw + sinRoll * cosPitch * sinYaw, //y
                cosRoll * cosPitch * sinYaw - sinRoll * sinPitch * cosYaw, //z
                cosRoll * cosPitch * cosYaw + sinRoll * sinPitch * sinYaw); //formerly yzx
    }

    public static Quat4d yrp2q_marian(double roll, double pitch, double yaw) {

        // Assuming the angles are in radians.
        double cosYawHalf = cos(yaw / 2);
        double sinYawHalf = sin(yaw / 2);
        double cosPitchHalf = cos(pitch / 2);
        double sinPitchHalf = sin(pitch / 2);
        double cosRollHalf = cos(roll / 2);
        double sinRollHalf = sin(roll / 2);
        double cosYawPitchHalf = cosYawHalf * cosPitchHalf;
        double sinYawPitchHalf = sinYawHalf * sinPitchHalf;

        return new Quat4d((cosYawPitchHalf * cosRollHalf - sinYawPitchHalf * sinRollHalf),
                (cosYawPitchHalf * sinRollHalf + sinYawPitchHalf * cosRollHalf),
                (sinYawHalf * cosPitchHalf * cosRollHalf + cosYawHalf * sinPitchHalf * sinRollHalf),
                (cosYawHalf * sinPitchHalf * cosRollHalf - sinYawHalf * cosPitchHalf * sinRollHalf));
    }

    private void provide() throws InterruptedException, TransformerException, TransformerFactory.TransformerFactoryException {
        publisher = getInstance().createTransformPublisher("java_provider");

        ////////////////
        // STATIC
        ////////////////
        // Define the translation
        Vector3d translation = new Vector3d(0.0, 1.0, 1.0);

        // Define the rotation
        //Quat4f rotation = yrp2q(0.0f, 0.0f, 0.0f);
        Quat4f rotation = yrp2q(0.0f, 0.0f, (float) -PI / 2);

        float rot_val = (float) PI / 2;
        double rot_val_d = PI / 2;

        out.println("Using translation: " + translation);
        out.println("Using rotation   : " + rotation);
        // Create the transformation
        Transform3D transform = new Transform3D(rotation, translation, 1.0);
        Transform transform_base_java = new Transform(transform, "base", "java_static", currentTimeMillis());

        // Publish the static offset
        out.println("Sending static transform now ...");
        publisher.sendTransform(transform_base_java, STATIC);

        ////////////////
        // DYNAMIC
        ////////////////
        // translate further along the original y axis
        Vector3d translation_dyn = new Vector3d(-1, 0, -2);
        // Define the rotation
        Quat4f rotation_dyn = yrp2q(0.0f, 0.0f, (float) -PI / 2);

        // Create the transformation
        Transform3D transform_dyn = new Transform3D(rotation_dyn, translation_dyn, 1.0);

        out.println("Sending dynamic transform now ...");

        // Publish the static offset
        while (true) {
            Transform transform_java_java_dynamic = new Transform(transform_dyn, "java_static", "java_dynamic", currentTimeMillis());
            publisher.sendTransform(transform_java_java_dynamic, DYNAMIC);
            sleep(20);
        }

    }

    private Vector4d transform_now(String source, String target, Vector4d point) throws InterruptedException, TransformerException {
        long when = currentTimeMillis();
        sleep(30);

        if (receiver.canTransform(target, source, when)) {
            Transform trafo = receiver.lookupTransform(target, source, when);
            out.println("[" + trafo.getFrameChild() + "]  -->  " + "[" + trafo.getFrameParent() + "]");

            Vector4d point4d = new Vector4d(point.x, point.y, point.z, 1.0);
            trafo.getTransform().transform(point4d);
            return point4d;

        } else {
            out.println("Error: Cannot transfrom " + source + " --> " + target);
            return new Vector4d();
        }

    }

    private void test() throws InterruptedException, TransformerException, TransformerFactory.TransformerFactoryException {
        receiver = getInstance().createTransformReceiver();

        out.println("Gathering available transformations ...");
        sleep(1000);

        // The different systems and the base
        String[] targets = {"java_static", "java_dynamic", "python_static", "python_dynamic"};
        String base = "base";

        Vector4d point = new Vector4d(1.0, 1.0, 1.0, 1.0);
        out.println("Point to transform: " + point + "\n");

        for (String target : targets) {
            // Forward transfrom
            Vector4d transformed_point = transform_now(base, target, point);
            out.println("[" + point + "]  -->  " + "[" + transformed_point + "]");

            // Backward transfrom
            Vector4d re_transformed_point = transform_now(target, base, transformed_point);
            out.println("[" + transformed_point + "]  -->  " + "[" + re_transformed_point + "]");

            out.println("");
        }
        exit(0);
    }

    public static void usage() {
        out.println("ERROR: Please provide task argument!");
        out.println("");
        out.println("Usage: program [TASK_ARGUMENT] (where TASK_ARGUMENT is either 'provide' or 'test')");
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            String start_type = args[0];
            try {
                Cross_lib_provider prov = new Cross_lib_provider();
                switch (start_type) {
                    case "provide":
                        prov.provide();
                        break;
                    case "test":
                        prov.test();
                        break;
                    default:
                        out.println("Unknown type: " + start_type);
                        usage();
                        break;
                }
            } catch (InterruptedException | TransformerException | TransformerFactory.TransformerFactoryException ex) {
                getLogger(Cross_lib_provider.class.getName()).log(SEVERE, null, ex);
            }
        } else {
            out.println("No arg given ...");
            usage();
        }
    }

}
