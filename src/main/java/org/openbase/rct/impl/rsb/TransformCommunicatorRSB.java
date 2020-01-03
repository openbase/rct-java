package org.openbase.rct.impl.rsb;

/*-
 * #%L
 * RCT
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rsb.com.RSBFactoryImpl;
import org.openbase.jul.extension.rsb.com.RSBSharedConnectionConfig;
import org.openbase.jul.extension.rsb.iface.RSBInformer;
import org.openbase.jul.extension.rsb.iface.RSBListener;
import org.openbase.jul.schedule.WatchDog;
import org.openbase.rct.*;
import org.openbase.rct.impl.TransformCommunicator;
import org.openbase.rct.impl.TransformListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.*;
import rsb.config.ParticipantConfig;
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

    private RSBInformer<Transform> rsbInformerTransform;
    private RSBInformer<Void> rsbInformerSync;
    private RSBListener rsbListenerTransform;
    private RSBListener rsbListenerSync;

    private WatchDog rsbInformerTransformWatchDog;
    private WatchDog rsbInformerSyncWatchDog;
    private WatchDog rsbListenerTransformWatchDog;
    private WatchDog rsbListenerSyncWatchDog;

    static {
        // Register converter for the FrameTransform type.
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new TransformConverter());
    }

    public TransformCommunicatorRSB(String name) {
        this.name = name;
    }

    @Override
    public void init(final TransformerConfig conf) throws TransformerException {
        try {

            LOGGER.debug("init communication");

            final ParticipantConfig participantConfig = RSBSharedConnectionConfig.getParticipantConfig();

            this.rsbInformerTransform = RSBFactoryImpl.getInstance().createSynchronizedInformer(RCT_SCOPE_TRANSFORM, Transform.class, participantConfig);
            this.rsbInformerSync = RSBFactoryImpl.getInstance().createSynchronizedInformer(RCT_SCOPE_SYNC, Void.class, participantConfig);
            this.rsbListenerTransform = RSBFactoryImpl.getInstance().createSynchronizedListener(RCT_SCOPE_TRANSFORM, participantConfig);
            this.rsbListenerSync = RSBFactoryImpl.getInstance().createSynchronizedListener(RCT_SCOPE_SYNC, participantConfig);

            this.rsbInformerTransformWatchDog = new WatchDog(rsbInformerTransform, "RSBInformerTransform");
            this.rsbInformerSyncWatchDog = new WatchDog(rsbInformerSync, "RSBInformerSync");
            this.rsbListenerTransformWatchDog = new WatchDog(rsbListenerTransform, "RSBListenerTransform");
            this.rsbListenerSyncWatchDog = new WatchDog(rsbListenerSync, "RSBListenerSync");

            this.rsbListenerTransform.addHandler(this::transformCallback, true);
            this.rsbListenerSync.addHandler(this::syncCallback, true);

            this.rsbInformerTransformWatchDog.activate();
            this.rsbInformerSyncWatchDog.activate();
            this.rsbListenerTransformWatchDog.activate();
            this.rsbListenerSyncWatchDog.activate();

            this.rsbInformerTransformWatchDog.waitForServiceActivation();
            this.rsbInformerSyncWatchDog.waitForServiceActivation();
            this.rsbListenerTransformWatchDog.waitForServiceActivation();
            this.rsbListenerSyncWatchDog.waitForServiceActivation();

            this.requestSync();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        } catch (CouldNotPerformException ex) {
            throw new TransformerException("Can not initialize rsb communicator.", ex);
        }
    }

    public void requestSync() throws TransformerException {
        try {
            if (rsbInformerSync == null || !rsbInformerSync.isActive()) {
                throw new TransformerException("Rsb communicator is not initialized.");
            }

            LOGGER.debug("Sending sync request trigger from id " + rsbInformerSync.getId());

            // trigger other instances to send transforms
            Event ev = new Event(rsbInformerSync.getScope(), Void.class, null);
            rsbInformerSync.publish(ev);
        } catch (CouldNotPerformException ex) {
            throw new TransformerException("Can not send transforms!", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    @Override
    public void sendTransform(final Transform transform, final TransformType type) throws TransformerException {
        try {
            if (rsbInformerTransform == null || !rsbInformerTransform.isActive()) {
                throw new TransformerException("RSB interface is not initialized!");
            }

            String cacheKey = transform.getFrameParent() + transform.getFrameChild();

            LOGGER.debug("Publishing transform from " + rsbInformerTransform.getId());

            synchronized (lock) {
                Event event = new Event(Transform.class);
                event.setData(transform);


                if (transform.getAuthority() == null || transform.getAuthority().equals("")) {
                    transform.setAuthority(name);
                }

                event.getMetaData().setUserInfo(USER_INFO_AUTHORITY, transform.getAuthority());

                switch (type) {
                    case STATIC:
                        if (transform.equalsWithoutTime(sendCacheStatic.get(cacheKey))) {
                            if (transform.equalsWithoutTime(GlobalTransformReceiver.getInstance().lookupTransform(transform.getFrameParent(), transform.getFrameChild(), System.currentTimeMillis()))) {
                                LOGGER.debug("Publishing static transform from " + rsbInformerTransform.getId() + " done because Transformation[" + cacheKey + "] already known.");
                                // we are done if transformation is already known
                                return;
                            }
                            LOGGER.warn("Publishing static transform from " + rsbInformerTransform.getId() + " again because Transformation[" + cacheKey + "] sync failed.");
                        }
                        sendCacheStatic.put(cacheKey, transform);
                        event.setScope(new Scope(RCT_SCOPE_TRANSFORM_STATIC));
                        break;
                    case DYNAMIC:
                        if (transform.equals(sendCacheDynamic.get(cacheKey))) {
                            if (transform.equalsWithoutTime(GlobalTransformReceiver.getInstance().lookupTransform(transform.getFrameParent(), transform.getFrameChild(), System.currentTimeMillis()))) {
                                LOGGER.debug("Publishing dynamic transform from " + rsbInformerTransform.getId() + " done because Transformation[" + cacheKey + "] already known.");
                                // we are done if transformation is already known
                                return;
                            }
                            LOGGER.warn("Publishing dynamic transform from " + rsbInformerTransform.getId() + " again because Transformation[" + cacheKey + "] sync failed.");
                            return;
                        }
                        sendCacheDynamic.put(cacheKey, transform);
                        event.setScope(new Scope(RCT_SCOPE_TRANSFORM_DYNAMIC));
                        break;
                    default:
                        throw new TransformerException("Unknown TransformType: " + type.name());
                }

                LOGGER.debug("Publishing transform from " + rsbInformerTransform.getId() + " initiated.");
                rsbInformerTransform.publish(event);
            }
        } catch (CouldNotPerformException ex) {
            throw new TransformerException("Can not send transform: " + transform, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
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

        try {
            if (event.getId().getParticipantId().equals(rsbInformerTransform.getId())) {
                LOGGER.trace("Received transform from myself. Ignore. (id " + event.getId().getParticipantId() + ")");
                return;
            }
        } catch (NotAvailableException e) {
            // continue if id could not be validated...
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

        try {
            if (event.getId().getParticipantId().equals(rsbInformerSync.getId())) {
                LOGGER.trace("Received sync request from myself. Ignore. (id " + event.getId().getParticipantId() + ")");
                return;
            }
        } catch (NotAvailableException e) {
            // continue if id could not be validated...
        }

        // concurrently publish currently known transform cache
        executor.execute(this::publishCache);
    }

    private void publishCache() {

        try {
            LOGGER.debug("Publishing cache from " + rsbInformerTransform.getId());
            synchronized (lock) {
                for (String key : sendCacheDynamic.keySet()) {
                    Event event = new Event(Transform.class);
                    event.setData(sendCacheDynamic.get(key));
                    event.getMetaData().setUserInfo(USER_INFO_AUTHORITY, sendCacheDynamic.get(key).getAuthority());
                    event.setScope(new Scope(RCT_SCOPE_TRANSFORM_DYNAMIC));
                    event.setType(Transform.class);

                    try {
                        rsbInformerTransform.publish(event);
                    } catch (CouldNotPerformException ex) {
                        throw new CouldNotPerformException("Can not publish cached dynamic transform " + sendCacheDynamic.get(key) + ".", ex);
                    }

                    // apply workaround to avoid sending to many events at once,
                    // because otherwise spread is killing some sessions.
                    // todo: implement more efficient by sending a collection instead of all transformations one by one.
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                for (String key : sendCacheStatic.keySet()) {
                    Event event = new Event(Transform.class);
                    event.setData(sendCacheStatic.get(key));
                    event.getMetaData().setUserInfo(USER_INFO_AUTHORITY, sendCacheStatic.get(key).getAuthority());
                    event.setScope(new Scope(RCT_SCOPE_TRANSFORM_STATIC));
                    event.setType(Transform.class);
                    try {
                        rsbInformerTransform.publish(event);
                    } catch (CouldNotPerformException ex) {
                        throw new CouldNotPerformException("Can not publish cached static transform " + sendCacheDynamic.get(key) + ".", ex);
                    }

                    // apply workaround to avoid sending to many events at once,
                    // because otherwise spread is killing some sessions.
                    // todo: implement more efficient by sending a collection instead of all transformations one by one.
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not publish all transformations!", ex, LOGGER);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {

        if (rsbListenerTransformWatchDog != null) {
            rsbListenerTransformWatchDog.shutdown();
        }

        if (rsbListenerSyncWatchDog != null) {
            rsbListenerSyncWatchDog.shutdown();
        }

        if (rsbInformerTransformWatchDog != null) {
            rsbInformerTransformWatchDog.shutdown();
        }

        if (rsbInformerSyncWatchDog != null) {
            rsbInformerSyncWatchDog.shutdown();
        }
    }
}
