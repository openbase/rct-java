package rct.impl.rsb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rct.TransformType;
import rct.TransformerConfig;
import rct.TransformerException;
import rct.impl.TransformCommunicator;
import rct.impl.TransformListener;
import rsb.*;
import rsb.converter.DefaultConverterRepository;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransformCommunicatorRSB implements TransformCommunicator {

    public static final String RCT_SCOPE_TRANSFORM = "/rct/transform";
    public static final String RCT_SCOPE_SYNC = "/rct/sync";
    private static final String RCT_SCOPE_SUFFIX_STATIC = "static";
    private static final String RCT_SCOPE_SUFFIX_DYNAMIC = "dynamic";
    private static final String RCT_SCOPE_SEPARATOR = "/";
    public static final String RCT_SCOPE_TRANSFORM_STATIC = RCT_SCOPE_TRANSFORM + RCT_SCOPE_SEPARATOR + RCT_SCOPE_SUFFIX_STATIC;
    public static final String RCT_SCOPE_TRANSFORM_DYNAMIC = RCT_SCOPE_TRANSFORM + RCT_SCOPE_SEPARATOR + RCT_SCOPE_SUFFIX_DYNAMIC;
    private static final String USER_INFO_AUTHORITY = "authority";
    private static final Logger LOGGER = LoggerFactory.getLogger(TransformCommunicatorRSB.class);
    private final Set<TransformListener> listeners = new HashSet<>();
    private final Map<String, Transform> sendCacheDynamic = new HashMap<>();
    private final Map<String, Transform> sendCacheStatic = new HashMap<>();
    private final Object lock = new Object();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final String name;
    private Listener rsbListenerTransform;
    private Informer<Transform> rsbInformerTransform;
    private Listener rsbListenerSync;
    private Informer<Void> rsbInformerSync;

    public TransformCommunicatorRSB(String name) {
        this.name = name;
    }

    @Override
    public void init(final TransformerConfig conf) throws TransformerException {

        LOGGER.debug("registering converter");

        // Register converter for the FrameTransform type.
        final TransformConverter converter = new TransformConverter();
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(converter);

        try {
            rsbListenerTransform = Factory.getInstance().createListener(RCT_SCOPE_TRANSFORM);
            rsbListenerSync = Factory.getInstance().createListener(RCT_SCOPE_SYNC);
            rsbInformerTransform = Factory.getInstance().createInformer(RCT_SCOPE_TRANSFORM);
            rsbInformerSync = Factory.getInstance().createInformer(RCT_SCOPE_SYNC);
            rsbListenerTransform.activate();
            rsbListenerSync.activate();
            rsbInformerTransform.activate();
            rsbInformerSync.activate();

        } catch (InitializeException ex) {
            throw new TransformerException("Can not initialize rsb communicator. Reason: " + ex.getMessage(), ex);
        } catch (RSBException ex) {
            throw new TransformerException("Can not initialize rsb communicator. Reason: " + ex.getMessage(), ex);
        }

        try {
            rsbListenerTransform.addHandler(this::transformCallback, true);
            rsbListenerSync.addHandler(this::syncCallback, true);
        } catch (InterruptedException ex) {
            throw new TransformerException("Can not initialize rsb communicator. Reason: " + ex.getMessage(), ex);
        }

        requestSync();
    }

    public void requestSync() throws TransformerException {
        if (rsbInformerSync == null || !rsbInformerSync.isActive()) {
            throw new TransformerException("Rsb communicator is not initialized.");
        }

        LOGGER.debug("Sending sync request trigger from id " + rsbInformerSync.getId());

        // trigger other instances to send transforms
        Event ev = new Event(rsbInformerSync.getScope(), Void.class, null);
        try {
            rsbInformerSync.publish(ev);
        } catch (RSBException ex) {
            throw new TransformerException("Can not trigger to send transforms. Reason: "
                    + ex.getMessage(), ex);
        }
    }

    @Override
    public void sendTransform(final Transform transform, final TransformType type) throws TransformerException {
        if (rsbInformerTransform == null || !rsbInformerTransform.isActive()) {
            throw new TransformerException("RSB interface is not initialized!");
        }

        String cacheKey = transform.getFrameParent() + transform.getFrameChild();

        LOGGER.debug("Publishing transform from " + rsbInformerTransform.getId());

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
                rsbInformerTransform.publish(event);
            } catch (RSBException ex) {
                throw new TransformerException("Can not send transform: "
                        + transform + ". Reason: " + ex.getMessage(), ex);
            }
        }
    }

    @Override
    public void sendTransform(Set<Transform> transforms, TransformType type)
            throws TransformerException {
        for (Transform t : transforms) {
            sendTransform(t, type);
        }
    }

    @Override
    public void addTransformListener(TransformListener listener) {
        synchronized (lock) {
            this.listeners.add(listener);
        }
    }

    @Override
    public void addTransformListener(Set<TransformListener> listeners) {
        synchronized (lock) {
            this.listeners.addAll(listeners);
        }
    }

    @Override
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
            LOGGER.warn("Received non-rct type on rct scope.");
            return;
        }
        if (event.getId().getParticipantId().equals(rsbInformerTransform.getId())) {
            LOGGER.trace("Received transform from myself. Ignore. (id " + event.getId().getParticipantId() + ")");
            return;
        }

        String authority = event.getMetaData().getUserInfo(USER_INFO_AUTHORITY);
        List<String> scopeComponents = event.getScope().getComponents();
        boolean isStatic = !scopeComponents.contains(RCT_SCOPE_SUFFIX_DYNAMIC);

        Transform transform = (Transform) event.getData();
        transform.setAuthority(authority);
        LOGGER.trace("Received transform from " + authority);
        LOGGER.debug("Received transform: " + transform + " - static: " + isStatic);

        synchronized (lock) {
            for (TransformListener l : listeners) {
                l.newTransformAvailable(transform, isStatic);
            }
        }
    }

    private void syncCallback(Event event) {
        if (event.getId().getParticipantId().equals(rsbInformerSync.getId())) {
            LOGGER.trace("Received sync request from myself. Ignore. (id " + event.getId().getParticipantId() + ")");
            return;
        }
        // concurrently publish currently known transform cache
        executor.execute(this::publishCache);
    }

    private void publishCache() {
        LOGGER.debug("Publishing cache from " + rsbInformerTransform.getId());
        synchronized (lock) {
            for (String key : sendCacheDynamic.keySet()) {
                Event event = new Event();
                event.setData(sendCacheDynamic.get(key));
                event.getMetaData().setUserInfo(USER_INFO_AUTHORITY, sendCacheDynamic.get(key).getAuthority());
                event.setScope(new Scope(RCT_SCOPE_TRANSFORM_DYNAMIC));
                event.setType(Transform.class);

                try {
                    rsbInformerTransform.publish(event);
                } catch (RSBException ex) {
                    LOGGER.error("Can not publish cached dynamic transform " + sendCacheDynamic.get(key) + ". Reason: " + ex.getMessage(), ex);
                }
            }
            for (String key : sendCacheStatic.keySet()) {
                Event event = new Event();
                event.setData(sendCacheStatic.get(key));
                event.getMetaData().setUserInfo(USER_INFO_AUTHORITY, sendCacheStatic.get(key).getAuthority());
                event.setScope(new Scope(RCT_SCOPE_TRANSFORM_STATIC));
                event.setType(Transform.class);
                try {
                    rsbInformerTransform.publish(event);
                } catch (RSBException ex) {
                    LOGGER.error("Can not publish cached static transform " + sendCacheDynamic.get(key) + ". Reason: " + ex.getMessage(), ex);
                }
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
                    if (rsbListenerTransform.isActive()) {
                        rsbListenerTransform.deactivate();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            } catch (RSBException | InterruptedException ex) {
                LOGGER.error("Can not deactivate rsb listener. Reason: " + ex.getMessage());
            }
        }

        if (rsbListenerSync != null) {
            try {
                try {
                    if (rsbListenerSync.isActive()) {
                        rsbListenerSync.deactivate();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            } catch (RSBException | InterruptedException ex) {
                LOGGER.error("Can not deactivate rsb listener. Reason: " + ex.getMessage());
            }
        }

        if (rsbInformerTransform != null) {
            try {
                try {
                    if (rsbInformerTransform.isActive()) {
                        rsbInformerTransform.deactivate();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            } catch (RSBException | InterruptedException ex) {
                LOGGER.error("Can not deactivate rsb listener. Reason: " + ex.getMessage());
            }
        }

        if (rsbInformerSync != null) {
            try {
                try {
                    if (rsbInformerSync.isActive()) {
                        rsbInformerSync.deactivate();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            } catch (RSBException | InterruptedException ex) {
                LOGGER.error("Can not deactivate rsb listener. Reason: " + ex.getMessage());
            }
        }
    }
}
