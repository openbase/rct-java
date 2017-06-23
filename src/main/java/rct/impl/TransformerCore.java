package rct.impl;

import java.util.Set;
import java.util.concurrent.Future;

import rct.Transform;
import rct.TransformerException;

public interface TransformerCore extends TransformListener {

    /**
     * Clear all data
     */
    void clear();

    /**
     * Add transform information to the rct data structure
     *
     * @param transform The transform to store
     * @param isStatic Record this transform as a static transform. It will be good
     * across all time. (This cannot be changed after the first
     * call.)
     * @return True unless an error occurs
     * @throws TransformerException
     */
    boolean setTransform(Transform transform, boolean isStatic) throws TransformerException;

    /**
     * Get the transform between two frames by frame ID.
     *
     * @param targetFrame The frame to which data should be transformed
     * @param sourceFrame The frame where the data originated
     * @param time The time at which the value of the transform is desired. (0 will get the latest)
     * @return The transform between the frames
     * @throws TransformerException
     *
     */
    Transform lookupTransform(String targetFrame, String sourceFrame, long time) throws TransformerException;

    /**
     * Get the transform between two frames by frame ID assuming fixed frame.
     *
     * @param targetFrame The frame to which data should be transformed
     * @param targetTime The time to which the data should be transformed. (0 will get the latest)
     * @param sourceFrame The frame where the data originated
     * @param sourceTime The time at which the source_frame should be evaluated. (0 will get the latest)
     * @param fixedFrame The frame in which to assume the transform is ant in time.
     * @return The transform between the frames
     * @throws TransformerException
     *
     */
    Transform lookupTransform(String targetFrame, long targetTime, String sourceFrame, long sourceTime, String fixedFrame) throws TransformerException;

    /**
     * Request the transform between two frames by frame ID.
     *
     * @param targetFrame The frame to which data should be transformed
     * @param sourceFrame The frame where the data originated
     * @param time The time at which the value of the transform is desired. (0 will get the latest)
     * @return A future object representing the request status and transform between the frames
     * @throws rct.TransformerException
     *
     */
    Future<Transform> requestTransform(String targetFrame, String sourceFrame, long time) throws TransformerException;

    /**
     * Test if a transform is possible
     *
     * @param targetFrame The frame into which to transform
     * @param sourceFrame The frame from which to transform
     * @param time The time at which to transform
     * @return True if the transform is possible, false otherwise
     */
    boolean canTransform(String targetFrame, String sourceFrame, long time);

    /**
     * Test if a transform is possible
     *
     * @param targetFrame The frame into which to transform
     * @param targetTime The time into which to transform
     * @param sourceFrame The frame from which to transform
     * @param sourceTime The time from which to transform
     * @param fixedFrame The frame in which to treat the transform as ant in time
     * @return True if the transform is possible, false otherwise
     */
    boolean canTransform(String targetFrame, long targetTime, String sourceFrame, long sourceTime, String fixedFrame);

    /**
     * A way to get a set of available frame ids
     *
     * @return
     */
    Set<String> getFrameStrings();

    /**
     * @brief Check if a frame exists in the tree
     * @param frameId The frame id in question
     * @return if the frame with the id exists
     */
    boolean frameExists(String frameId);

    /**
     * @brief Fill the parent of a frame.
     * @param time
     * @param frame_id The frame id of the frame in question
     * @throws TransformerException
     * @return
     */
    String getParent(String frame_id, long time) throws TransformerException;

    /**
     * Backwards compatability. A way to see what frames have been cached Useful
     * for debugging
     *
     * @return
     */
    String allFramesAsDot();

    /**
     * A way to see what frames have been cached in yaml format Useful for
     * debugging tools
     *
     * @return
     */
    String allFramesAsYAML();

    /**
     * A way to see what frames have been cached Useful for debugging
     *
     * @return
     */
    String allFramesAsString();
}
