package rct;

import java.util.concurrent.Future;

import rct.impl.TransformCommunicator;
import rct.impl.TransformerCore;

/**
 * This is the central class for receiving transforms. Use
 * {@link TransformerFactory} to create an instance of this. Any instance should
 * exist as long as any receiving action is planned, because it
 * caches the known coordinate frames tree including the defined history.
 * Creation of the frame tree creates overhead and should not be done on a
 * regular basis. Instead the transformer should exist for a longer period while
 * updating itself when changes to the frame tree occur.
 * 
 * @author lziegler
 *
 */
public class TransformReceiver {

	private TransformerCore core;
	private TransformerConfig conf;
	
	@SuppressWarnings("unused")
	private TransformCommunicator comm;

	/**
	 * Creates a new transformer. Attention: This should not be called by the
	 * user, use {@link TransformerFactory} in order to create a transformer.
	 * 
	 * @param core
	 *            The core functionality implementation
	 * @param comm
	 *            The communicator implementation
	 * @param conf
	 *            The configuration
	 */
	public TransformReceiver(TransformerCore core, TransformCommunicator comm,
			TransformerConfig conf) {
		this.core = core;
		this.conf = conf;
		this.comm = comm;
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
	 * @throws TransformerException
	 *
	 */
	public Transform lookupTransform(String target_frame, String source_frame,
			long time) throws TransformerException {
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
	 *            The frame in which to assume the transform is constant in
	 *            time.
	 * @return The transform between the frames
	 * @throws TransformerException
	 */
	public Transform lookupTransform(String target_frame, long target_time,
			String source_frame, long source_time, String fixed_frame)
			throws TransformerException {
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
	public boolean canTransform(String target_frame, String source_frame,
			long time) {
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
    
    /** 
     * Shutdown the transform communicator
     */
    public void shutdown() {
        comm.shutdown();
    }
}
