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

	/** \brief Add transform information to the rct data structure
	 * \param transform The transform to store
	 * \param authority The source of the information for this transform
	 * \param is_static Record this transform as a static transform.  It will be good across all time.  (This cannot be changed after the first call.)
	 * @throws TransformerException 
	 */
	public void sendTransform(Transform transform, TransformType type) throws TransformerException;

	/** \brief Add transform information to the rct data structure
	 * \param transform The transform to store
	 * \param authority The source of the information for this transform
	 * \param is_static Record this transform as a static transform.  It will be good across all time.  (This cannot be changed after the first call.)
	 * @throws TransformerException 
	 */
	public void sendTransform(Set<Transform> transforms, TransformType type) throws TransformerException;

	public void addTransformListener(TransformListener listener);
	public void addTransformListener(Set<TransformListener> listeners);
	public void removeTransformListener(TransformListener listener);

	public String getAuthorityID();
}
