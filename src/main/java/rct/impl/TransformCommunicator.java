package rct.impl;

import java.util.Set;

import rct.Transform;
import rct.TransformerConfig;

public interface TransformCommunicator {
	
	public void init(TransformerConfig conf);

	/** \brief Add transform information to the rct data structure
	 * \param transform The transform to store
	 * \param authority The source of the information for this transform
	 * \param is_static Record this transform as a static transform.  It will be good across all time.  (This cannot be changed after the first call.)
	 * \return True unless an error occured
	 */
	public boolean sendTransform(Transform transform, boolean isStatic);

	/** \brief Add transform information to the rct data structure
	 * \param transform The transform to store
	 * \param authority The source of the information for this transform
	 * \param is_static Record this transform as a static transform.  It will be good across all time.  (This cannot be changed after the first call.)
	 * \return True unless an error occured
	 */
	public boolean sendTransform(Set<Transform> transforms, boolean isStatic);

	public void addTransformListener(TransformListener listener);
	public void addTransformListener(Set<TransformListener> listeners);
	public void removeTransformListener(TransformListener listener);

	public String getAuthorityName();
}
