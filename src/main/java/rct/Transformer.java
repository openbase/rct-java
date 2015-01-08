package rct;

import java.util.Set;
import java.util.concurrent.Future;

import rct.impl.TransformCommunicator;
import rct.impl.TransformerCore;

public class Transformer {

	private TransformerCore core;
	private TransformCommunicator comm;
	private TransformerConfig conf;

	public Transformer(TransformerCore core, TransformCommunicator comm,
			TransformerConfig conf) {
		this.core = core;
		this.conf = conf;
		this.comm = comm;
	}

	/**
	 * @brief Add transform information to the rct data structure
	 * @param transform
	 *            The transform to store
	 * @param authority
	 *            The source of the information for this transform
	 * @param isStatic
	 *            Record this transform as a static transform. It will be good
	 *            across all time. (This cannot be changed after the first
	 *            call.)
	 * @return True unless an error occured
	 */
	public boolean sendTransform(Transform transform, boolean isStatic) {
		return comm.sendTransform(transform, isStatic);
	}

	/**
	 * @brief Add transform information to the rct data structure
	 * @param transform
	 *            The transform to store
	 * @param authority
	 *            The source of the information for this transform
	 * @param is_static
	 *            Record this transform as a static transform. It will be good
	 *            across all time. (This cannot be changed after the first
	 *            call.)
	 * @return True unless an error occured
	 */
	public boolean sendTransform(Set<Transform> transforms, boolean isStatic) {
		return comm.sendTransform(transforms, isStatic);
	}

	/**
	 * @brief Get the transform between two frames by frame ID.
	 * @param target_frame
	 *            The frame to which data should be transformed
	 * @param source_frame
	 *            The frame where the data originated
	 * @param time
	 *            The time at which the value of the transform is desired. (0
	 *            will get the latest)
	 * @return The transform between the frames
	 *
	 */
	public Transform lookupTransform(String target_frame, String source_frame,
			long time) {
		return core.lookupTransform(target_frame, source_frame, time);
	}

	/**
	 * @brief Get the transform between two frames by frame ID assuming fixed
	 *        frame.
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
	 */
	public Transform lookupTransform(String target_frame, long target_time,
			String source_frame, long source_time, String fixed_frame) {
		return core.lookupTransform(target_frame, target_time, source_frame,
				source_time, fixed_frame);
	}

	/**
	 * @brief Request the transform between two frames by frame ID.
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
	public Future<Transform> requestTransform(String target_frame,
			String source_frame, long time) {
		return core.requestTransform(target_frame, source_frame, time);
	}

	/**
	 * @brief Test if a transform is possible
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
	public boolean canTransform(String target_frame, String source_frame, long time) {
		return core.canTransform(target_frame, source_frame, time);
	}

	/**
	 * @brief Test if a transform is possible
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
	public boolean canTransform(String target_frame, long target_time,
			String source_frame, long source_time, String fixed_frame) {
		return core.canTransform(target_frame, target_time, source_frame,
				source_time, fixed_frame);
	}

	public TransformerConfig getConfig() {
		return conf;
	}

	public String getAuthorityName() {
		return comm.getAuthorityName();
	}

}
