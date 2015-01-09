package rct.impl.rsb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;

import rct.Transform;
import rct.TransformerConfig;
import rct.impl.TransformCommunicator;
import rct.impl.TransformListener;
import rct.proto.FrameTransformType.FrameTransform;
import rsb.Event;
import rsb.Factory;
import rsb.Handler;
import rsb.Informer;
import rsb.InitializeException;
import rsb.Listener;
import rsb.RSBException;
import rsb.Scope;
import rst.geometry.RotationType.Rotation;
import rst.geometry.TranslationType.Translation;
import rst.timing.TimestampType.Timestamp;

public class TransformCommunicatorRSB implements TransformCommunicator {

	public static final String RCT_SCOPE_TRANSFORM = "/rct/transform";
	public static final String RCT_SCOPE_TRIGGER = "/rct/trigger";

	private Listener rsbListenerTransform;
	private Informer<FrameTransform> rsbInformerTransform;
	private Listener rsbListenerTrigger;
	private Informer<Void> rsbInformerTrigger;
	private Set<TransformListener> listeners = new HashSet<TransformListener>();

	private Map<String, FrameTransform> sendCache = new HashMap<String, FrameTransform>();
	private Map<String, FrameTransform> sendCacheStatic = new HashMap<String, FrameTransform>();
	private Object lock = new Object();
	ExecutorService executor = Executors.newCachedThreadPool();

	private static Logger logger = Logger
			.getLogger(TransformCommunicatorRSB.class);

	public TransformCommunicatorRSB() {
	}

	public void init(TransformerConfig conf) throws TransformerException {

		try {
			rsbListenerTransform = Factory.getInstance().createListener(
					RCT_SCOPE_TRANSFORM);
			rsbListenerTrigger = Factory.getInstance().createListener(
					RCT_SCOPE_TRIGGER);
			rsbInformerTransform = Factory.getInstance().createInformer(
					RCT_SCOPE_TRANSFORM);
			rsbInformerTrigger = Factory.getInstance().createInformer(
					RCT_SCOPE_TRIGGER);
			rsbListenerTransform.activate();
			rsbListenerTrigger.activate();
			rsbInformerTransform.activate();
			rsbInformerTrigger.activate();

		} catch (InitializeException e) {
			throw new TransformerException("Can not initialize rsb communicator. Reason: " + e.getMessage(), e);
		} catch (RSBException e) {
			throw new TransformerException("Can not initialize rsb communicator. Reason: " + e.getMessage(), e);
		}

		try {
			rsbListenerTransform.addHandler(new Handler() {
				public void internalNotify(Event event) {
					frameTransformCallback(event);
				}
			}, true);
			rsbListenerTrigger.addHandler(new Handler() {
				public void internalNotify(Event event) {
					triggerCallback(event);
				}
			}, true);
		} catch (InterruptedException e) {
			throw new TransformerException("Can not initialize rsb communicator. Reason: " + e.getMessage(), e);
		}
		
		requestSync();
	}

	public void requestSync() throws TransformerException {
		if (rsbInformerTrigger == null || !rsbInformerTrigger.isActive()) {
			throw new TransformerException("Rsb communicator is not initialized.");
		}

		logger.debug("Sending sync request trigger from id " + rsbInformerTrigger.getId());

		// trigger other instances to send transforms
		Event ev = new Event(rsbInformerTrigger.getScope(), Void.class, null);
		try {
			rsbInformerTrigger.send(ev);
		} catch (RSBException e) {
			throw new TransformerException("Can not trigger to send transforms. Reason: " + e.getMessage(), e);
		}
	}

	public boolean sendTransform(Transform transform, boolean isStatic)
			throws TransformerException {
		if (rsbInformerTransform == null || !rsbInformerTransform.isActive()) {
			throw new TransformerException("RSB interface is not initialized!");
		}

		FrameTransform t = convertTransformToPb(transform);
		String cacheKey = transform.getFrameParent()
				+ transform.getFrameChild();

		logger.debug("Publishing transform from "
				+ rsbInformerTransform.getId());

		synchronized (lock) {
			Event event = new Event();
			event.setData(t);
			event.setType(FrameTransform.class);
			if (isStatic) {
				sendCacheStatic.put(cacheKey, t);
				event.setScope(new Scope(RCT_SCOPE_TRANSFORM + "/static"));
			} else {
				sendCache.put(cacheKey, t);
				event.setScope(new Scope(RCT_SCOPE_TRANSFORM + "/nonstatic"));
			}
			try {
				rsbInformerTransform.send(event);
			} catch (RSBException e) {
				throw new TransformerException("Can not send transform: "
						+ transform + ". Reason: " + e.getMessage(), e);
			}
		}
		return true;
	}

	public boolean sendTransform(Set<Transform> transforms, boolean isStatic) throws TransformerException {
		boolean ret = true;
		for (Transform t : transforms) {
			ret &= sendTransform(t, isStatic);
		}
		return ret;
	}

	public void addTransformListener(TransformListener listener) {
		synchronized (lock) {
			this.listeners.add(listener);
		}
	}

	public void addTransformListener(Set<TransformListener> listeners) {
		synchronized (lock) {
			this.listeners.addAll(listeners);
		}
	}

	public void removeTransformListener(TransformListener listener) {
		synchronized (lock) {
			this.listeners.remove(listener);
		}
	}

	public String getAuthorityName() {
		return rsbInformerTransform.getId().toString();
	}

	private void frameTransformCallback(Event event) {
		if (event.getType() != FrameTransform.class) {
			logger.warn("Received non-rct type on rct scope.");
			return;
		}
		if (event.getId().getParticipantId()
				.equals(rsbInformerTransform.getId())) {
			logger.warn("Received transform from myself. Ignore. (id "
					+ event.getId().getParticipantId() + ")");
			return;
		}

		FrameTransform t = (FrameTransform) event.getData();

		String authority = event.getId().getParticipantId().toString();
		List<String> scopeComponents = event.getScope().getComponents();
		boolean isStatic = !scopeComponents.contains("nonstatic");

		Transform transform = convertPbToTransform(t);
		transform.setAuthority(authority);
		logger.debug("Received transform from " + authority);
		logger.debug("Received transform: " + transform);

		synchronized (lock) {
			for (TransformListener l : listeners) {
				l.newTransformAvailable(transform, isStatic);
			}
		}
	}

	private void triggerCallback(Event event) {
		if (event.getId().getParticipantId().equals(rsbInformerTrigger.getId())) {
			logger.warn("Received transform from myself. Ignore. (id "
					+ event.getId().getParticipantId() + ")");
			return;
		}
		// concurrently publish currently known transform cache
		executor.execute(new Runnable() {
			public void run() {
				publishCache();
			}
		});
	}

	private void publishCache() {
		logger.debug("Publishing cache from " + rsbInformerTransform.getId());
		for (String key : sendCache.keySet()) {

			Event event = new Event();
			event.setData(sendCache.get(key));
			event.setScope(new Scope(RCT_SCOPE_TRANSFORM + "/nonstatic"));
			event.setType(FrameTransform.class);
			try {
				rsbInformerTransform.send(event);
			} catch (RSBException e) {
				logger.error(
						"Can not publish cached transform "
								+ sendCache.get(key) + ". Reason: "
								+ e.getMessage(), e);
			}
		}
		for (String key : sendCacheStatic.keySet()) {
			Event event = new Event();
			event.setData(sendCacheStatic.get(key));
			event.setScope(new Scope(RCT_SCOPE_TRANSFORM + "/static"));
			event.setType(FrameTransform.class);
			try {
				rsbInformerTransform.send(event);
			} catch (RSBException e) {
				logger.error(
						"Can not publish cached transform "
								+ sendCache.get(key) + ". Reason: "
								+ e.getMessage(), e);
			}
		}
	}

	public static FrameTransform convertTransformToPb(Transform t) {

		long timeMSec = t.getTime();
		long timeUSec = timeMSec * 1000l;

		Quat4d quat = t.getRotationQuat();
		Vector3d vec = t.getTranslation();

		FrameTransform.Builder tBuilder = FrameTransform.newBuilder();
		tBuilder.getTimeBuilder().setTime(timeUSec);
		tBuilder.setFrameChild(t.getFrameChild());
		tBuilder.setFrameParent(t.getFrameParent());
		tBuilder.getTransformBuilder().getRotationBuilder().setQw(quat.w);
		tBuilder.getTransformBuilder().getRotationBuilder().setQx(quat.x);
		tBuilder.getTransformBuilder().getRotationBuilder().setQy(quat.y);
		tBuilder.getTransformBuilder().getRotationBuilder().setQz(quat.z);
		tBuilder.getTransformBuilder().getTranslationBuilder().setX(vec.x);
		tBuilder.getTransformBuilder().getTranslationBuilder().setY(vec.y);
		tBuilder.getTransformBuilder().getTranslationBuilder().setZ(vec.z);

		return tBuilder.build();
	}

	public static Transform convertPbToTransform(FrameTransform t) {

		Timestamp time = t.getTime();
		long timeUSec = time.getTime();
		long timeMSec = timeUSec / 1000l;

		Rotation rstRot = t.getTransform().getRotation();
		Translation rstTrans = t.getTransform().getTranslation();

		Quat4d quat = new Quat4d(rstRot.getQx(), rstRot.getQy(),
				rstRot.getQz(), rstRot.getQw());
		Vector3d vec = new Vector3d(rstTrans.getX(), rstTrans.getY(),
				rstTrans.getZ());

		Transform3D transform3d = new Transform3D(quat, vec, 1.0);

		Transform newTrans = new Transform(transform3d, t.getFrameParent(),
				t.getFrameChild(), timeMSec);
		return newTrans;
	}
}
