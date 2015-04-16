# rct-java

Robotics Coordinate Transform (Java)

## Installation

    mvn package install

## Usage

### Sending Transforms

    public static void main(String[] args) {

        // Get the rct factory
        TransformerFactory factory = TransformerFactory.getInstance();

        // Create the publisher object
        TransformPublisher publisher = factory.createTransformPublisher("example");

        // Create the mathematical homogeneous transformation (rotation, translation, scale)
        Quat4f rotation = new Quat4f(1, 0, 0, 1);
        Vector3d translation = new Vector3d(1, 2, 3);
        Transform3D transform = new Transform3D(rotation, translation, 1.0);

        // Create the rct transform object with source and target frames
        Transform t = new Transform(transform, "foo", "bar", System.currentTimeMillis());

        // Publish the transform object
        publisher.sendTransform(t, TransformType.STATIC);
    }

### Receiving Transforms Synchronously

    private TransformReceiver receiver;

    public static void main(String[] args) {

        // Get the rct factory
        TransformerFactory factory = TransformerFactory.getInstance();

        // Create a receiver object. Should be live as long as the application runs
        receiver = factory.createTransformReceiver();

        // The receiver needs some time to receive the coordinate system tree
        doStuff();

        // Lookup the transform
        Transform t = transformer.lookupTransform("foo", "bar", System.currentTimeMillis());

        System.out.println(t);
    }

### Receiving Transforms Asynchronously

    private TransformReceiver receiver;

    public static void main(String[] args) {

        // Get the rct factory
        TransformerFactory factory = TransformerFactory.getInstance();

        // Create a receiver object. Should be live as long as the application runs
        receiver = factory.createTransformReceiver();

        // Request a transform. Will return immediately
        Future<Transform> future = transformer.requestTransform("foo", "bar", System.currentTimeMillis());

        // Wait or poll
        Transform t = null;
        while (t == null) {
            t = future.get(100);

            doStuff();
        }

        System.out.println(t);
    }
