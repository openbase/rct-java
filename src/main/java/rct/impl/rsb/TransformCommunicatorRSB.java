package rct.impl.rsb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.j3d.Transform3D;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

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

	private static Logger logger = Logger.getLogger(TransformCommunicatorRSB.class);
	
	public TransformCommunicatorRSB() {
	}

	public void init(TransformerConfig conf) {
		
		try {
			rsbListenerTransform = Factory.getInstance().createListener(RCT_SCOPE_TRANSFORM);
			rsbListenerTrigger = Factory.getInstance().createListener(RCT_SCOPE_TRIGGER);
			rsbInformerTransform = Factory.getInstance().createInformer(RCT_SCOPE_TRANSFORM);
			rsbInformerTrigger = Factory.getInstance().createInformer(RCT_SCOPE_TRIGGER);
		} catch (InitializeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			rsbListenerTransform.addHandler(new Handler() {
				public void internalNotify(Event event) {
			        if (event.getType() != FrameTransform.class) {
			            logger.warn("Received non-rct type on rct scope.");
			            return;
			        }
			        if (event.getId().getParticipantId().equals(rsbInformerTransform.getId())) {
			        	logger.warn("Received transform from myself. Ignore. (id " + event.getId().getParticipantId() + ")");
			            return;
			        }
			        frameTransformCallback(event);
				}
			}, true);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean sendTransform(Transform transform, boolean isStatic) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean sendTransform(Set<Transform> transforms, boolean isStatic) {
		// TODO Auto-generated method stub
		return false;
	}

	public void addTransformListener(TransformListener listener) {
		this.listeners.add(listener);
	}

	public void addTransformListener(Set<TransformListener> listeners) {
		this.listeners.addAll(listeners);
	}

	public void removeTransformListener(TransformListener listener) {
		this.listeners.remove(listener);
	}

	public String getAuthorityName() {
		return rsbInformerTransform.getId().toString();
	}
	
	private void frameTransformCallback(Event event) {
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

	private void triggerCallback(Event t) {
		
	}
	private void publishCache() {
		
	}

	public static Transform convertPbToTransform(FrameTransform t) {
		
		Timestamp time = t.getTime();
		long timeUSec = time.getTime();
		long timeMSec = timeUSec / 1000l;

		Rotation rstRot = t.getTransform().getRotation();
		Translation rstTrans = t.getTransform().getTranslation();
		
		Quat4d quat = new Quat4d(rstRot.getQx(), rstRot.getQy(), rstRot.getQz(), rstRot.getQw());
		Vector3d vec = new Vector3d(rstTrans.getX(), rstTrans.getY(), rstTrans.getZ());

		Transform3D transform3d = new Transform3D(quat, vec, 1.0);

		Transform newTrans = new Transform(transform3d, t.getFrameParent(), t.getFrameChild(), timeMSec);
		return newTrans;
	}
}
