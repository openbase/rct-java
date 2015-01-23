package rct.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.media.j3d.Transform3D;
import javax.vecmath.Matrix3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.apache.log4j.Logger;

import rct.Transform;
import rct.TransformerException;
import rct.impl.TransformCache.TimeAndFrameID;
import rct.impl.TransformRequest.FutureTransform;

public class TransformerCoreDefault implements TransformerCore {

	private enum WalkEnding {
		Identity, TargetParentOfSource, SourceParentOfTarget, FullPath,
	};

	private interface TransformAccum {
		public int gather(TransformCache cache, long time);

		public void accum(boolean source);

		public void finalize(WalkEnding end, long _time);
	}

	private class TransformAccumDummy implements TransformAccum {
		public int gather(TransformCache cache, long time) {
			return cache.getParent(time);
		}

		public void accum(boolean source) {
		}

		public void finalize(WalkEnding end, long _time) {
		}
	};

	private class TransformAccumImpl implements TransformAccum {

		private TransformInternal st = new TransformInternal();
		private long time = 0;
		private Quat4d source_to_top_quat = new Quat4d(0.0, 0.0, 0.0, 1.0);
		private Vector3d source_to_top_vec = new Vector3d(0, 0, 0);
		private Quat4d target_to_top_quat = new Quat4d(0, 0, 0, 1);
		private Vector3d target_to_top_vec = new Vector3d(0, 0, 0);
		private Quat4d result_quat = new Quat4d(0.0, 0.0, 0.0, 1.0);
		private Vector3d result_vec = new Vector3d(0, 0, 0);

		public TransformAccumImpl() {
		}

		public Vector3d quatRotate(Quat4d r, Vector3d v) {
			Matrix3d rotMat = new Matrix3d();
			rotMat.set(r);
			Vector3d result = new Vector3d();
			rotMat.transform(v, result);
			return result;
		}

		public int gather(TransformCache cache, long time) {
			if (!cache.getData(time, st)) {
				return 0;
			}
			return st.frame_id;
		}

		public void accum(boolean source) {
			if (source) {
				source_to_top_vec = quatRotate(st.rotation, source_to_top_vec);
				source_to_top_vec.add(st.translation);
				source_to_top_quat.mul(st.rotation, source_to_top_quat);
			} else {
				target_to_top_vec = quatRotate(st.rotation, target_to_top_vec);
				target_to_top_vec.add(st.translation);
				target_to_top_quat.mul(st.rotation, target_to_top_quat);
			}
		}

		public void finalize(WalkEnding end, long _time) {
			switch (end) {
			case Identity:
				break;
			case TargetParentOfSource:
				result_vec = source_to_top_vec;
				result_quat = source_to_top_quat;
				break;
			case SourceParentOfTarget: {
				Quat4d inv_target_quat = new Quat4d(target_to_top_quat);
				inv_target_quat.inverse();
				Vector3d target_to_top_vec_neg = new Vector3d(target_to_top_vec);
				target_to_top_vec_neg.negate();
				Vector3d inv_target_vec = quatRotate(inv_target_quat,
						target_to_top_vec_neg);
				result_vec = inv_target_vec;
				result_quat = inv_target_quat;
				break;
			}
			case FullPath: {
				Quat4d inv_target_quat = new Quat4d(target_to_top_quat);
				inv_target_quat.inverse();
				Vector3d target_to_top_vec_neg = new Vector3d(target_to_top_vec);
				target_to_top_vec_neg.negate();
				Vector3d inv_target_vec = quatRotate(inv_target_quat,
						target_to_top_vec_neg);
				result_vec = quatRotate(inv_target_quat, source_to_top_vec);
				result_vec.add(inv_target_vec);
				result_quat.mul(inv_target_quat, source_to_top_quat);
			}
				break;
			}
			time = _time;
		}
	}

	private static final int MAX_GRAPH_DEPTH = 1000;

	private static Logger logger = Logger
			.getLogger(TransformerCoreDefault.class);
	private Object lock = new Object();
	private Map<String, Integer> frameIds = new HashMap<String, Integer>();
	private List<TransformCache> frames = new LinkedList<TransformCache>();
	private List<String> frameIdsReverse = new LinkedList<String>();
	private Map<Integer, String> frameAuthority = new HashMap<Integer, String>();
	private long cacheTime;
	private Set<TransformRequest> requests = new HashSet<TransformRequest>();
	private List<TimeAndFrameID> lctCache = new LinkedList<TimeAndFrameID>();

	private Executor executor = Executors.newCachedThreadPool();

	public TransformerCoreDefault(long cacheTime) {
		this.cacheTime = cacheTime;
		frameIds.put("NO_PARENT", 0);
		frames.add(new TransformCacheNull());
		frameIdsReverse.add("NO_PARENT");
	}

	public void clear() {
		synchronized (lock) {
			if (frames.size() > 1) {
				for (TransformCache f : frames) {
					if (f.isValid())
						f.clearList();
				}
			}
		}
	}

	public boolean setTransform(Transform transform, boolean is_static)
			throws TransformerException {

		// prepare data
		String authority = transform.getAuthority();
		String frameChild = transform.getFrameChild().replace("/", "").trim();
		String frameParent = transform.getFrameParent().replace("/", "").trim();
		Quat4d quat = transform.getRotationQuat();
		Vector3d vec = transform.getTranslation();
		Transform stripped = new Transform(transform);
		stripped.setFrameChild(frameChild);
		stripped.setFrameParent(frameParent);

		// check input data validity
		if (frameChild.equals(frameParent)) {
			logger.error("Frames for parent and child are the same: "
					+ frameChild);
			throw new TransformerException(
					"Frames for parent and child are the same: " + frameChild);
		}
		if (frameChild.isEmpty()) {
			logger.error("Child frame is empty");
			throw new TransformerException("Child frame is empty");
		}
		if (frameParent.isEmpty()) {
			logger.error("Parent frame is empty");
			throw new TransformerException("Parent frame is empty");
		}
		if (Double.isNaN(quat.w) || Double.isNaN(quat.x)
				|| Double.isNaN(quat.y) || Double.isNaN(quat.z)
				|| Double.isNaN(vec.x) || Double.isNaN(vec.y)
				|| Double.isNaN(vec.z)) {
			logger.error("Transform contains nan: " + transform);
			throw new TransformerException("Transform contains nan: "
					+ transform);
		}

		// perform the insertion
		synchronized (lock) {
			logger.debug("lookup child frame number");
			int frameNumberChild = lookupOrInsertFrameNumber(frameChild);
			logger.debug("get frame \"" + frameNumberChild + "\"");
			TransformCache frame = getFrame(frameNumberChild);
			if (!frame.isValid()) {
				logger.debug("allocate frame " + frameNumberChild);
				frame = allocateFrame(frameNumberChild, is_static);
			}

			logger.debug("lookup parent frame number");
			int frameNumberParent = lookupOrInsertFrameNumber(stripped
					.getFrameParent());
			logger.debug("insert transform " + frameNumberParent + " -> "
					+ frameNumberChild + " to " + frame);
			if (frame.insertData(new TransformInternal(stripped,
					frameNumberParent, frameNumberChild))) {
				logger.debug("transform inserted. Add authority.");
				frameAuthority.put(frameNumberChild, authority);
			} else {
				logger.warn("TF_OLD_DATA ignoring data from the past for frame "
						+ stripped.getFrameChild()
						+ " at time "
						+ stripped.getTime()
						+ " according to authority "
						+ authority
						+ "\nPossible reasons are listed at http://wiki.ros.org/tf/Errors%%20explained");
				return false;
			}
		}
		logger.debug("trigger check requests.");
		executor.execute(new Runnable() {
			public void run() {
				checkRequests();
			}
		});
		logger.debug("set transform done");
		return true;
	}

	private int lookupOrInsertFrameNumber(String frameid_str) {
		int retval = 0;
		if (!frameIds.containsKey(frameid_str)) {
			logger.debug("frame id is not known for string \"" + frameid_str
					+ "\"");
			retval = frames.size();
			logger.debug("add null transform to cache");
			frames.add(new TransformCacheNull());
			logger.debug("generated mapping \"" + frameid_str + "\" -> "
					+ retval + " (and reverse)");
			frameIds.put(frameid_str, retval);
			frameIdsReverse.add(frameid_str);
		} else {
			retval = frameIds.get(frameid_str);
			logger.debug("known mapping \"" + frameid_str + "\" -> " + retval);
		}

		return retval;
	}

	private TransformCache getFrame(int frame_id) {
		// / @todo check larger values too
		if (frame_id == 0 || frame_id > frames.size())
			return null;
		else {
			return frames.get(frame_id);
		}
	}

	private TransformCache allocateFrame(int cfid, boolean is_static) {
		if (is_static) {
			frames.set(cfid, new TransformCacheStatic());
		} else {
			frames.set(cfid, new TransformCacheImpl(cacheTime));
		}

		return frames.get(cfid);
	}

	public Transform lookupTransform(String target_frame, String source_frame,
			long time) throws TransformerException {
		synchronized (lock) {

			if (target_frame == source_frame) {

				long newTime = 0;
				if (time == 0) {
					int target_id = lookupFrameNumber(target_frame);
					TransformCache cache = getFrame(target_id);
					if (cache.isValid())
						newTime = cache.getLatestTimestamp();
					else
						newTime = time;
				} else
					newTime = time;

				Transform3D t = new Transform3D();
				Transform identity = new Transform(t, target_frame,
						source_frame, newTime);
				return identity;
			}

			return lookupTransformNoLock(target_frame, source_frame, time);
		}
	}

	private Transform lookupTransformNoLock(String target_frame,
			String source_frame, long time) throws TransformerException {

		// Identify case does not need to be validated above
		int target_id = validateFrameId(
				"lookupTransform argument target_frame", target_frame);
		int source_id = validateFrameId(
				"lookupTransform argument source_frame", source_frame);

		TransformAccumImpl accum = new TransformAccumImpl();
		try {
			walkToTopParent(accum, time, target_id, source_id);
		} catch(TransformerException e) {
			throw new TransformerException("No matching transform found", e);
		}

		Transform3D t3d = new Transform3D(accum.result_quat, accum.result_vec,
				1.0);
		Transform output_transform = new Transform(t3d, target_frame,
				source_frame, accum.time);
		return output_transform;
	}

	private void walkToTopParent(TransformAccum f, long time, int target_id,
			int source_id) throws TransformerException {
		// Short circuit if zero length transform to allow lookups on non
		// existant links
		if (source_id == target_id) {
			f.finalize(WalkEnding.Identity, time);
			return;
		}

		// If getting the latest get the latest common time
		if (time == 0) {
			getLatestCommonTime(target_id, source_id, time);
		}

		// Walk the tree to its root from the source frame, accumulating the
		// transform
		int frame = source_id;
		int top_parent = frame;
		int depth = 0;

		boolean extrapolation_might_have_occurred = false;

		while (frame != 0) {
			TransformCache cache = getFrame(frame);

			if (!cache.isValid()) {
				// There will be no cache for the very root of the tree
				top_parent = frame;
				break;
			}

			int parent = f.gather(cache, time);
			if (parent == 0) {
				// Just break out here... there may still be a path from source
				// -> target
				top_parent = frame;
				extrapolation_might_have_occurred = true;
				break;
			}

			// Early out... target frame is a direct parent of the source frame
			if (frame == target_id) {
				f.finalize(WalkEnding.TargetParentOfSource, time);
				return;
			}

			f.accum(true);

			top_parent = frame;
			frame = parent;

			++depth;
			if (depth > MAX_GRAPH_DEPTH) {
				throw new TransformerException(
						"The tf tree is invalid because it contains a loop.");
			}
		}

		// Now walk to the top parent from the target frame, accumulating its
		// transform
		frame = target_id;
		depth = 0;
		while (frame != top_parent) {
			TransformCache cache = getFrame(frame);

			if (!cache.isValid()) {
				break;
			}

			int parent = f.gather(cache, time);
			if (parent == 0) {
				throw new TransformerException(
						"when looking up transform from frame ["
								+ lookupFrameString(source_id) + "] to frame ["
								+ lookupFrameString(target_id) + "]");
			}

			// Early out... source frame is a direct parent of the target frame
			if (frame == source_id) {
				f.finalize(WalkEnding.SourceParentOfTarget, time);
				return;
			}

			f.accum(false);
			frame = parent;
			++depth;
			if (depth > MAX_GRAPH_DEPTH) {
				throw new TransformerException(
						"The tf tree is invalid because it contains a loop."
								+ allFramesAsStringNoLock());
			}
		}

		if (frame != top_parent) {
			if (extrapolation_might_have_occurred) {
				throw new TransformerException(
						", when looking up transform from frame ["
								+ lookupFrameString(source_id) + "] to frame ["
								+ lookupFrameString(target_id) + "]");

			}
		}

		f.finalize(WalkEnding.FullPath, time);

		return;
	}

	private void getLatestCommonTime(int target_id, int source_id, long time)
			throws TransformerException {
		if (source_id == target_id) {
			TransformCache cache = getFrame(source_id);
			// Set time to latest timestamp of frameid in case of target and
			// source frame id are the same
			if (cache.isValid())
				time = cache.getLatestTimestamp();
			else
				time = 0;
			return;
		}

		lctCache.clear();

		// Walk the tree to its root from the source frame, accumulating the
		// list of parent/time as well as the latest time
		// in the target is a direct parent
		int frame = source_id;
		int depth = 0;
		long common_time = Long.MAX_VALUE;
		while (frame != 0) {
			TransformCache cache = getFrame(frame);

			if (!cache.isValid()) {
				// There will be no cache for the very root of the tree
				break;
			}

			TimeAndFrameID latest = cache.getLatestTimeAndParent();

			if (latest.frameID == 0) {
				// Just break out here... there may still be a path from source
				// -> target
				break;
			}

			if (latest.time != 0) {
				common_time = Math.min(latest.time, common_time);
			}

			lctCache.add(latest);

			frame = latest.frameID;

			// Early out... target frame is a direct parent of the source frame
			if (frame == target_id) {
				time = common_time;
				if (time == Long.MAX_VALUE) {
					time = 0;
				}
				return;
			}

			++depth;
			if (depth > MAX_GRAPH_DEPTH) {
				throw new TransformerException(
						"The tf tree is invalid because it contains a loop."
								+ allFramesAsStringNoLock());

			}
		}

		// Now walk to the top parent from the target frame, accumulating the
		// latest time and looking for a common parent
		frame = target_id;
		depth = 0;
		common_time = Long.MAX_VALUE;
		int common_parent = 0;
		while (true) {
			TransformCache cache = getFrame(frame);

			if (!cache.isValid()) {
				break;
			}

			TimeAndFrameID latest = cache.getLatestTimeAndParent();

			if (latest.frameID == 0) {
				break;
			}

			if (latest.time != 0) {
				common_time = Math.min(latest.time, common_time);
			}

			boolean found = false;
			for (TimeAndFrameID t : lctCache) {
				if (t.frameID == latest.frameID) {
					found = true;
					break;
				}
			}
			if (found) { // found a common parent
				common_parent = latest.frameID;
				break;
			}

			frame = latest.frameID;

			// Early out... source frame is a direct parent of the target frame
			if (frame == source_id) {
				time = common_time;
				if (time == Long.MAX_VALUE) {
					time = 0;
				}
				return;
			}

			++depth;
			if (depth > MAX_GRAPH_DEPTH) {
				throw new TransformerException(
						"The tf tree is invalid because it contains a loop."
								+ allFramesAsStringNoLock());
			}
		}

		if (common_parent == 0) {
			throw new TransformerException(
					"Could not find a connection between '"
							+ lookupFrameString(target_id) + "' and '"
							+ lookupFrameString(source_id)
							+ "' because they are not part of the same tree."
							+ "Tf has two or more unconnected trees.");
		}

		// Loop through the source -> root list until we hit the common parent
		for (TimeAndFrameID it : lctCache) {
			if (it.time != 0) {
				common_time = Math.min(common_time, it.time);
			}

			if (it.frameID == common_parent) {
				break;
			}
		}

		if (common_time == Long.MAX_VALUE) {
			common_time = 0;
		}

		time = common_time;
		return;
	}

	private String lookupFrameString(int frame_id_num)
			throws TransformerException {
		if (frame_id_num >= frameIdsReverse.size()) {
			throw new TransformerException("Reverse lookup of frame id "
					+ frame_id_num + " failed!");
		} else
			return frameIdsReverse.get(frame_id_num);
	}

	private int lookupFrameNumber(String frameid_str) {
		int retval;
		if (!frameIds.containsKey(frameid_str)) {
			retval = 0;
		} else
			retval = frameIds.get(frameid_str);
		return retval;
	}

	private int validateFrameId(String function_name_arg, String frame_id)
			throws TransformerException {
		if (frame_id.isEmpty()) {
			throw new TransformerException("Invalid argument passed to "
					+ function_name_arg + " in tf2 frame_ids cannot be empty");
		}

		if (frame_id.startsWith("/")) {
			throw new TransformerException("Invalid argument \"" + frame_id
					+ "\" passed to " + function_name_arg
					+ " in tf2 frame_ids cannot start with a '/' like: ");
		}

		int id = lookupFrameNumber(frame_id);
		if (id == 0) {
			throw new TransformerException("\"" + frame_id + "\" passed to "
					+ function_name_arg + " does not exist. ");
		}

		return id;
	}

	public Transform lookupTransform(String target_frame, long target_time,
			String source_frame, long source_time, String fixed_frame)
			throws TransformerException {

		validateFrameId("lookupTransform argument target_frame", target_frame);
		validateFrameId("lookupTransform argument source_frame", source_frame);
		validateFrameId("lookupTransform argument fixed_frame", fixed_frame);

		Transform temp1 = lookupTransform(fixed_frame, source_frame,
				source_time);
		Transform temp2 = lookupTransform(target_frame, fixed_frame,
				target_time);

		Transform3D t = new Transform3D();
		t.mul(temp2.getTransform(), temp1.getTransform());

		return new Transform(t, target_frame, source_frame, temp2.getTime());
	}

	public Future<Transform> requestTransform(String target_frame,
			String source_frame, long time) {

		FutureTransform future = new FutureTransform();
		synchronized (lock) {
			int target_id = lookupFrameNumber(target_frame);
			int source_id = lookupFrameNumber(source_frame);
			if (canTransformNoLock(target_id, source_id, time)) {
				try {
					future.set(lookupTransformNoLock(target_frame,
							source_frame, time));
					return future;
				} catch (TransformerException e) {
					logger.warn("Lookup error: " + e.getMessage(), e);
				}
			}

			requests.add(new TransformRequest(target_frame, source_frame, time,
					future));
			return future;
		}
	}

	private void checkRequests() {
		// go through all request and check if they can be answered
		for (Iterator<TransformRequest> requestIt = requests.iterator(); requestIt
				.hasNext();) {
			TransformRequest request = requestIt.next();
			synchronized (lock) {
				try {
					Transform t = lookupTransformNoLock(request.target_frame,
							request.source_frame, request.time);
					// request can be answered. publish the transform through
					// the future object and remove the request.
					request.future.set(t);
					requestIt.remove();
				} catch (TransformerException e) {
					// expected, just proceed
					continue;
				}
			}
		}
	}

	public boolean canTransform(String target_frame, String source_frame,
			long time) {
		// Short circuit if target_frame == source_frame
		if (target_frame == source_frame)
			return true;

		if (warnFrameId("canTransform argument target_frame", target_frame))
			return false;
		if (warnFrameId("canTransform argument source_frame", source_frame))
			return false;

		synchronized (lock) {
			int target_id = lookupFrameNumber(target_frame);
			int source_id = lookupFrameNumber(source_frame);
			return canTransformNoLock(target_id, source_id, time);
		}
	}

	public boolean canTransform(String target_frame, long target_time,
			String source_frame, long source_time, String fixed_frame) {
		if (warnFrameId("canTransform argument target_frame", target_frame))
			return false;
		if (warnFrameId("canTransform argument source_frame", source_frame))
			return false;
		if (warnFrameId("canTransform argument fixed_frame", fixed_frame))
			return false;

		return canTransform(target_frame, fixed_frame, target_time)
				&& canTransform(fixed_frame, source_frame, source_time);
	}

	private boolean warnFrameId(String function_name_arg, String frame_id) {
		if (frame_id.length() == 0) {
			logger.warn("Invalid argument passed to " + function_name_arg
					+ " in tf2 frame_ids cannot be empty");
			return true;
		}

		if (frame_id.startsWith("/")) {
			logger.warn("Invalid argument \"" + frame_id + "\" passed to "
					+ function_name_arg
					+ " in tf2 frame_ids cannot start with a '/' like: ");
			return true;
		}

		return false;
	}

	public Set<String> getFrameStrings() {
		synchronized (lock) {
			Set<String> vec = new HashSet<String>();
			for (int counter = 1; counter < frameIdsReverse.size(); counter++) {
				vec.add(frameIdsReverse.get(counter));
			}
			return vec;
		}
	}

	public boolean frameExists(String frame_id_str) {
		synchronized (lock) {
			return frameIds.containsKey(frame_id_str);
		}
	}

	public String getParent(String frame_id, long time)
			throws TransformerException {
		synchronized (lock) {
			int frame_number = lookupFrameNumber(frame_id);
			TransformCache frame = getFrame(frame_number);

			if (!frame.isValid())
				return "";

			int parent_id = frame.getParent(time);
			if (parent_id == 0)
				return "";

			return lookupFrameString(parent_id);
		}
	}

	public String allFramesAsDot() {
		String mstream = "";
		mstream += "digraph G {\n";
		synchronized (lock) {

			TransformInternal temp = new TransformInternal();

			if (frames.size() == 1) {
				mstream += "\"no tf data recieved\"";
			}

			// one referenced for 0 is no frame
			for (int counter = 1; counter < frames.size(); counter++) {
				int frame_id_num;
				TransformCache counter_frame = getFrame(counter);
				if (!counter_frame.isValid()) {
					continue;
				}
				if (!counter_frame.getData(0, temp)) {
					continue;
				} else {
					frame_id_num = temp.frame_id;
				}
				String authority = "no recorded authority";
				if (frameAuthority.containsKey(counter))
					authority = frameAuthority.get(counter);

				double rate = counter_frame.getListLength()
						/ Math.max(
								(counter_frame.getLatestTimestamp() / 1000.0 - counter_frame
										.getOldestTimestamp() / 1000.0), 0.0001);

				mstream += "\""
						+ frameIdsReverse.get(frame_id_num)
						+ "\" -> \""
						+ frameIdsReverse.get(counter)
						+ "\"[label=\"Broadcaster: "
						+ authority
						+ "\\nAverage rate: "
						+ rate
						+ " Hz\\nMost recent transform: "
						+ (counter_frame.getLatestTimestamp())
						/ 1000.0
						+ " \\nBuffer length: "
						+ (counter_frame.getLatestTimestamp() - counter_frame
								.getOldestTimestamp()) / 1000.0 + " sec\\n"
						+ "\"];\n";
			}

			// one referenced for 0 is no frame
			for (int counter = 1; counter < frames.size(); counter++) {
				int frame_id_num;
				TransformCache counter_frame = getFrame(counter);
				if (!counter_frame.isValid()) {
					continue;
				}
				if (counter_frame.getData(0, temp)) {
					frame_id_num = temp.frame_id;
				} else {
					frame_id_num = 0;
				}

				if (frameIdsReverse.get(frame_id_num).equals("NO_PARENT")) {
					mstream += "edge [style=invis];\n";
					mstream += " subgraph cluster_legend { style=bold; color=black; label =\"view_frames Result\";\n"
							+ "}->\"" + frameIdsReverse.get(counter) + "\";\n";
				}
			}
			mstream += "}";
			return mstream;
		}
	}

	public String allFramesAsYAML() {
		String mstream = "";
		synchronized (lock) {

			TransformInternal temp = new TransformInternal();

			if (frames.size() == 1)
				mstream += "[]";

			// for (std::vector< TimeCache*>::iterator it = frames_.begin(); it
			// != frames_.end(); ++it)
			for (int counter = 1; counter < frames.size(); counter++) {
				// one referenced for 0 is no frame
				int cfid = counter;
				int frame_id_num;
				TransformCache cache = getFrame(cfid);
				if (!cache.isValid()) {
					continue;
				}

				if (!cache.getData(0, temp)) {
					continue;
				}

				frame_id_num = temp.frame_id;

				String authority = "no recorded authority";
				if (frameAuthority.containsKey(cfid)) {
					authority = frameAuthority.get(cfid);
				}

				double rate = cache.getListLength()
						/ Math.max((cache.getLatestTimestamp() / 1000.0 - cache
								.getOldestTimestamp() / 1000.0), 0.0001);

				mstream += frameIdsReverse.get(cfid) + ": \n";
				mstream += "  parent: '" + frameIdsReverse.get(frame_id_num)
						+ "'\n";
				mstream += "  broadcaster: '" + authority + "'\n";
				mstream += "  rate: " + rate + "\n";
				mstream += "  most_recent_transform: "
						+ (cache.getLatestTimestamp()) / 1000.0 + "\n";
				mstream += "  oldest_transform: "
						+ (cache.getOldestTimestamp()) / 1000.0 + "\n";
				mstream += "  buffer_length: "
						+ (cache.getLatestTimestamp() - cache
								.getOldestTimestamp()) / 1000.0 + "\n";
			}

			return mstream;
		}
	}

	public String allFramesAsString() {
		synchronized (lock) {
			return allFramesAsStringNoLock();
		}
	}

	private boolean canTransformNoLock(int target_id, int source_id, long time) {
		if (target_id == 0 || source_id == 0) {
			return false;
		}

		if (target_id == source_id) {
			return true;
		}

		TransformAccumDummy accum = new TransformAccumDummy();
		try {
			walkToTopParent(accum, time, target_id, source_id);
		} catch (TransformerException e) {
			return false;
		}
		return true;

	}

	private String allFramesAsStringNoLock() {

		TransformInternal temp = new TransformInternal();
		String mstring = "";

		// /regular transforms
		logger.debug("frames size: " + frames.size());
		for (int counter = 1; counter < frames.size(); counter++) {
			TransformCache frame_ptr = getFrame(counter);
			logger.debug("got frame: " + frame_ptr);
			if (!frame_ptr.isValid())
				continue;
			int frame_id_num = 0;
			if (frame_ptr.getData(0, temp)) {
				logger.debug("got frame transform: " + temp);
				frame_id_num = temp.frame_id;
			}

			mstring += "Frame " + frameIdsReverse.get(counter)
					+ " exists with parent "
					+ frameIdsReverse.get(frame_id_num) + ".\n";
		}
		return mstring;
	}

	public void newTransformAvailable(Transform transform, boolean isStatic) {
		try {
			setTransform(transform, isStatic);
		} catch (TransformerException e) {
			logger.error(e.getMessage());
			logger.debug(e);
		}
	}
}
