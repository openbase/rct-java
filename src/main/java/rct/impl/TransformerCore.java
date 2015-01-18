package rct.impl;

import java.util.Set;
import java.util.concurrent.Future;

import rct.Transform;
import rct.TransformerException;

public interface TransformerCore {

	/** Clear all data */
	void clear();

	/**
	 * Add transform information to the rct data structure
	 * 
	 * @param transform
	 *            The transform to store
	 * @param authority
	 *            The source of the information for this transform
	 * @param is_static
	 *            Record this transform as a static transform. It will be good
	 *            across all time. (This cannot be changed after the first
	 *            call.)
	 * @return True unless an error occured
	 * @throws TransformerException 
	 */
	boolean setTransform(Transform transform, boolean is_static) throws TransformerException;

	/**
	 * Get the transform between two frames by frame ID.
	 * 
	 * @param target_frame
	 *            The frame to which data should be transformed
	 * @param source_frame
	 *            The frame where the data originated
	 * @param time
	 *            The time at which the value of the transform is desired. (0
	 *            will get the latest)
	 * @return The transform between the frames
	 * @throws TransformerException 
	 *
	 */
	Transform lookupTransform(String target_frame, String source_frame,
			long time) throws TransformerException;

	/**
	 * Get the transform between two frames by frame ID assuming fixed frame.
	 * 
	 * @param target_frame
	 *            The frame to which data should be transformed
	 * @param target_time
	 *            The time to which the data should be transformed. (0 will get
	 *            the latest)
	 * @param source_frame
	 *            The frame where the data originated
	 * @param source_time
	 *            The time at which the source_frame should be evaluated. (0
	 *            will get the latest)
	 * @param fixed_frame
	 *            The frame in which to assume the transform is ant in time.
	 * @return The transform between the frames
	 * @throws TransformerException 
	 *
	 */
	Transform lookupTransform(String target_frame, long target_time,
			String source_frame, long source_time, String fixed_frame) throws TransformerException;

	/**
	 * Request the transform between two frames by frame ID.
	 * 
	 * @param target_frame
	 *            The frame to which data should be transformed
	 * @param source_frame
	 *            The frame where the data originated
	 * @param time
	 *            The time at which the value of the transform is desired. (0
	 *            will get the latest)
	 * @return A future object representing the request status and transform
	 *         between the frames
	 *
	 */
	Future<Transform> requestTransform(String target_frame,
			String source_frame, long time);

	/**
	 * Test if a transform is possible
	 * 
	 * @param target_frame
	 *            The frame into which to transform
	 * @param source_frame
	 *            The frame from which to transform
	 * @param time
	 *            The time at which to transform
	 * @param error_msg
	 *            A pointer to a string which will be filled with why the
	 *            transform failed, if not NULL
	 * @return True if the transform is possible, false otherwise
	 */
	boolean canTransform(String target_frame, String source_frame, long time);

	/**
	 * Test if a transform is possible
	 * 
	 * @param target_frame
	 *            The frame into which to transform
	 * @param target_time
	 *            The time into which to transform
	 * @param source_frame
	 *            The frame from which to transform
	 * @param source_time
	 *            The time from which to transform
	 * @param fixed_frame
	 *            The frame in which to treat the transform as ant in time
	 * @param error_msg
	 *            A pointer to a string which will be filled with why the
	 *            transform failed, if not NULL
	 * @return True if the transform is possible, false otherwise
	 */
	boolean canTransform(String target_frame, long target_time,
			String source_frame, long source_time, String fixed_frame);

	/** A way to get a std::vector of available frame ids */
	Set<String> getFrameStrings();

	/**
	 * @brief Check if a frame exists in the tree
	 * @param frame_id_str
	 *            The frame id in question
	 */
	boolean frameExists(String frame_id_str);

	/**
	 * @brief Fill the parent of a frame.
	 * @param frame_id
	 *            The frame id of the frame in question
	 * @throws TransformerException 
	 */
	String getParent(String frame_id, long time) throws TransformerException;

	/**
	 * Backwards compatability. A way to see what frames have been cached Useful
	 * for debugging
	 */
	String allFramesAsDot();

	/**
	 * A way to see what frames have been cached in yaml format Useful for
	 * debugging tools
	 */
	String allFramesAsYAML();

	/**
	 * A way to see what frames have been cached Useful for debugging
	 */
	String allFramesAsString();
}
