package rct;

import java.util.Set;

import rct.impl.TransformCommunicator;

/**
 * This is the central class for publishing transforms. Use
 * {@link TransformerFactory} to create an instance of this. Any instance should
 * exist as long as any publishing is planned.
 *
 * @author lziegler
 *
 */
public class TransformPublisher {

    private TransformCommunicator comm;
    private TransformerConfig conf;

    /**
     * Creates a new transformer. Attention: This should not be called by the
     * user, use {@link TransformerFactory} in order to create a transformer.
     *
     * @param comm
     * The communicator implementation
     * @param conf
     * The configuration
     */
    public TransformPublisher(TransformCommunicator comm, TransformerConfig conf) {
        this.conf = conf;
        this.comm = comm;
    }

    /**
     * @brief Add transform information to the rct data structure
     * @param transform  The transform to store
     * @param isStatic Record this transform as a static transform. It will be good across all time. (This cannot be changed after the first call.) 
     * @throws TransformerException
     */
    public void sendTransform(Transform transform, TransformType type)
            throws TransformerException {
        comm.sendTransform(transform, type);
    }

    /**
     * @brief Add transform information to the rct data structure
     * @param transform The transform to store
     * @param is_static Record this transform as a static transform. It will be good across all time. (This cannot be changed after the first call.)
     * @throws TransformerException
     */
    public void sendTransform(Set<Transform> transforms, TransformType type) throws TransformerException {
        comm.sendTransform(transforms, type);
    }

    public TransformerConfig getConfig() {
        return conf;
    }

    public String getAuthorityID() {
        return comm.getAuthorityID();
    }

    /**
     * Shutdown the transform communicator
     */
    public void shutdown() {
        comm.shutdown();
    }

}
