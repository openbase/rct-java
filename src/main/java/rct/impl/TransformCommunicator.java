package rct.impl;

import java.util.Set;

import rct.Transform;
import rct.TransformType;
import rct.TransformerConfig;
import rct.TransformerException;

public interface TransformCommunicator {

    public void init(TransformerConfig conf) throws TransformerException;

    /**
     * Shutdown the transform communicator
     */
    public void shutdown();

    /**
     *
     * @param transform
     * @param type
     * @throws TransformerException
     */
    public void sendTransform(Transform transform, TransformType type) throws TransformerException;

    /**
     *
     * @param transforms
     * @param type
     * @throws TransformerException
     */
    public void sendTransform(Set<Transform> transforms, TransformType type) throws TransformerException;

    public void addTransformListener(TransformListener listener);

    public void addTransformListener(Set<TransformListener> listeners);

    public void removeTransformListener(TransformListener listener);

    public String getAuthorityID();
}
