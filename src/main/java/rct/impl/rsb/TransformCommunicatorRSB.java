package rct.impl.rsb;

import static java.lang.Thread.currentThread;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import static java.util.concurrent.Executors.newCachedThreadPool;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import rct.Transform;
import rct.TransformType;
import rct.TransformerConfig;
import rct.TransformerException;
import rct.impl.TransformCommunicator;
import rct.impl.TransformListener;
import rsb.Event;
import static rsb.Factory.getInstance;
import rsb.Informer;
import rsb.InitializeException;
import rsb.Listener;
import rsb.RSBException;
import rsb.Scope;
import static rsb.converter.DefaultConverterRepository.getDefaultConverterRepository;

public class TransformCommunicatorRSB implements TransformCommunicator {

    private static final String RCT_SCOPE_SUFFIX_STATIC = "static";
    private static final String RCT_SCOPE_SUFFIX_DYNAMIC = "dynamic";
    private static final String RCT_SCOPE_SEPARATOR = "/";
    public static final String RCT_SCOPE_TRANSFORM = "/rct/transform";
    public static final String RCT_SCOPE_TRANSFORM_STATIC = RCT_SCOPE_TRANSFORM + RCT_SCOPE_SEPARATOR + RCT_SCOPE_SUFFIX_STATIC;
    public static final String RCT_SCOPE_TRANSFORM_DYNAMIC = RCT_SCOPE_TRANSFORM + RCT_SCOPE_SEPARATOR + RCT_SCOPE_SUFFIX_DYNAMIC;
    public static final String RCT_SCOPE_SYNC = "/rct/sync";

    private static final String USER_INFO_AUTHORITY = "authority";

    private Listener rsbListenerTransform;
    private Informer<Transform> rsbInformerTransform;
    private Listener rsbListenerSync;
    private Informer<Void> rsbInformerSync;
    private Set<TransformListener> listeners = new HashSet<>();

    private Map<String, Transform> sendCacheDynamic = new HashMap<>();
    private Map<String, Transform> sendCacheStatic = new HashMap<>();
    private Object lock = new Object();
    private ExecutorService executor = newCachedThreadPool();
    private String name;

    private static Logger logger = getLogger(TransformCommunicatorRSB.class);

    public TransformCommunicatorRSB(String name) {
        this.name = name;
    }

    public void init(TransformerConfig conf) throws TransformerException {

        logger.debug("registering converter");

        // Register converter for the FrameTransform type.
        final TransformConverter converter = new TransformConverter();
        getDefaultConverterRepository().addConverter(converter);

        try {
            rsbListenerTransform = getInstance().createListener(RCT_SCOPE_TRANSFORM);
            rsbListenerSync = getInstance().createListener(RCT_SCOPE_SYNC);
            rsbInformerTransform = getInstance().createInformer(RCT_SCOPE_TRANSFORM);
            rsbInformerSync = getInstance().createInformer(RCT_SCOPE_SYNC);
            rsbListenerTransform.activate();
            rsbListenerSync.activate();
            rsbInformerTransform.activate();
            rsbInformerSync.activate();

        } catch (InitializeException e) {
            throw new TransformerException("Can not initialize rsb communicator. Reason: "
                    + e.getMessage(), e);
        } catch (RSBException e) {
            throw new TransformerException("Can not initialize rsb communicator. Reason: "
                    + e.getMessage(), e);
        }

        try {
            rsbListenerTransform.addHandler(this::transformCallback, true);
            rsbListenerSync.addHandler(this::syncCallback, true);
        } catch (InterruptedException e) {
            throw new TransformerException("Can not initialize rsb communicator. Reason: "
                    + e.getMessage(), e);
        }

        requestSync();
    }

    public void requestSync() throws TransformerException {
        if (rsbInformerSync == null || !rsbInformerSync.isActive()) {
            throw new TransformerException("Rsb communicator is not initialized.");
        }

        logger.debug("Sending sync request trigger from id " + rsbInformerSync.getId());

        // trigger other instances to send transforms
        Event ev = new Event(rsbInformerSync.getScope(), Void.class, null);
        try {
            rsbInformerSync.send(ev);
        } catch (RSBException e) {
            throw new TransformerException("Can not trigger to send transforms. Reason: "
                    + e.getMessage(), e);
        }
    }

    public void sendTransform(Transform transform, TransformType type) throws TransformerException {
        if (rsbInformerTransform == null || !rsbInformerTransform.isActive()) {
            throw new TransformerException("RSB interface is not initialized!");
        }

        String cacheKey = transform.getFrameParent() + transform.getFrameChild();

        logger.debug("Publishing transform from " + rsbInformerTransform.getId());

        synchronized (lock) {
            Event event = new Event();
            event.setData(transform);
            event.setType(Transform.class);
            if (transform.getAuthority() == null || transform.getAuthority().equals("")) {
                transform.setAuthority(name);
            }

            event.getMetaData().setUserInfo(USER_INFO_AUTHORITY, transform.getAuthority());

            switch (type) {
                case STATIC:
                    sendCacheStatic.put(cacheKey, transform);
                    event.setScope(new Scope(RCT_SCOPE_TRANSFORM_STATIC));
                    break;
                case DYNAMIC:
                    sendCacheDynamic.put(cacheKey, transform);
                    event.setScope(new Scope(RCT_SCOPE_TRANSFORM_DYNAMIC));
                    break;
                default:
                    throw new TransformerException("Unknown TransformType: " + type.name());
            }

            try {
                rsbInformerTransform.send(event);
            } catch (RSBException e) {
                throw new TransformerException("Can not send transform: " + transform + ". Reason: " + e.getMessage(), e);
            }
        }
    }

    public void sendTransform(Set<Transform> transforms, TransformType type)
            throws TransformerException {
        for (Transform t : transforms) {
            sendTransform(t, type);
        }
        return;
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

    @Override
    public String getAuthorityID() {
        return name;
    }

    private void transformCallback(Event event) {
        if (event.getType() != Transform.class) {
            logger.warn("Received non-rct type on rct scope.");
            return;
        }
        if (event.getId().getParticipantId().equals(rsbInformerTransform.getId())) {
            logger.trace("Received transform from myself. Ignore. (id " + event.getId().getParticipantId() + ")");
            return;
        }

        String authority = event.getMetaData().getUserInfo(USER_INFO_AUTHORITY);
        List<String> scopeComponents = event.getScope().getComponents();
        boolean isStatic = !scopeComponents.contains(RCT_SCOPE_SUFFIX_DYNAMIC);

        Transform transform = (Transform) event.getData();
        transform.setAuthority(authority);
        logger.trace("Received transform from " + authority);
        logger.debug("Received transform: " + transform + " - static: " + isStatic);

        synchronized (lock) {
            for (TransformListener l : listeners) {
                l.newTransformAvailable(transform, isStatic);
            }
        }
    }

    private void syncCallback(Event event) {
        if (event.getId().getParticipantId().equals(rsbInformerSync.getId())) {
            logger.trace("Received sync request from myself. Ignore. (id " + event.getId().getParticipantId() + ")");
            return;
        }
        // concurrently publish currently known transform cache
        executor.execute(this::publishCache);
    }

    private void publishCache() {
        logger.debug("Publishing cache from " + rsbInformerTransform.getId());
        for (String key : sendCacheDynamic.keySet()) {

            Event event = new Event();
            event.setData(sendCacheDynamic.get(key));
            event.getMetaData().setUserInfo(USER_INFO_AUTHORITY, sendCacheDynamic.get(key).getAuthority());
            event.setScope(new Scope(RCT_SCOPE_TRANSFORM_DYNAMIC));
            event.setType(Transform.class);
            try {
                rsbInformerTransform.send(event);
            } catch (RSBException e) {
                logger.error("Can not publish cached dynamic transform " + sendCacheDynamic.get(key) + ". Reason: " + e.getMessage(), e);
            }
        }
        for (String key : sendCacheStatic.keySet()) {
            Event event = new Event();
            event.setData(sendCacheStatic.get(key));
            event.getMetaData().setUserInfo(USER_INFO_AUTHORITY, sendCacheStatic.get(key).getAuthority());
            event.setScope(new Scope(RCT_SCOPE_TRANSFORM_STATIC));
            event.setType(Transform.class);
            try {
                rsbInformerTransform.send(event);
            } catch (RSBException e) {
                logger.error("Can not publish cached static transform " + sendCacheDynamic.get(key) + ". Reason: " + e.getMessage(), e);
            }
        }
    }

    /**
     * @{@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (rsbListenerTransform != null) {
            try {
                try {
                    rsbListenerTransform.deactivate();
                } catch (InterruptedException ex) {
                    currentThread().interrupt();
                    throw ex;
                }
            } catch (RSBException | InterruptedException e) {
                logger.error("Can not deactivate rsb listener. Reason: " + e.getMessage());
            }
        }

        if (rsbListenerSync != null) {
            try {
                try {
                    rsbListenerSync.deactivate();
                } catch (InterruptedException ex) {
                    currentThread().interrupt();
                    throw ex;
                }
            } catch (RSBException | InterruptedException e) {
                logger.error("Can not deactivate rsb listener. Reason: " + e.getMessage());
            }
        }

        if (rsbInformerTransform != null) {
            try {
                try {
                    rsbInformerTransform.deactivate();
                } catch (InterruptedException ex) {
                    currentThread().interrupt();
                    throw ex;
                }
            } catch (RSBException | InterruptedException e) {
                logger.error("Can not deactivate rsb listener. Reason: " + e.getMessage());
            }
        }

        if (rsbInformerSync != null) {
            try {
                try {
                    rsbInformerSync.deactivate();
                } catch (InterruptedException ex) {
                    currentThread().interrupt();
                    throw ex;
                }
            } catch (RSBException | InterruptedException e) {
                logger.error("Can not deactivate rsb listener. Reason: " + e.getMessage());
            }
        }
    }
}
