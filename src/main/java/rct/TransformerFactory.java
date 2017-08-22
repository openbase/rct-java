package rct;

import java.util.HashSet;
import java.util.Set;

import rct.impl.TransformCommunicator;
import rct.impl.TransformListener;
import rct.impl.TransformerCore;
import rct.impl.TransformerCoreDefault;
import rct.impl.rsb.TransformCommunicatorRSB;

public class TransformerFactory {

    private static TransformerFactory singleInstance = null;

    public static TransformerFactory getInstance() {
        if (singleInstance == null) {
            singleInstance = new TransformerFactory();
        }
        return singleInstance;
    }

    public static void killInstance() {
        singleInstance = null;
    }

    private TransformerFactory() {
    }

    public static class TransformerFactoryException extends Exception {

        private static final long serialVersionUID = 670357224688663291L;

        public TransformerFactoryException() {
            super();
        }

        public TransformerFactoryException(String msg) {
            super(msg);
        }

        public TransformerFactoryException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    public TransformReceiver createTransformReceiver() throws TransformerFactoryException {
        return createTransformReceiver(new TransformerConfig());
    }

    public TransformReceiver createTransformReceiver(TransformerConfig config) throws TransformerFactoryException {
        Set<TransformListener> listeners = new HashSet<TransformListener>();
        return createTransformReceiver(listeners, config);
    }

    public TransformReceiver createTransformReceiver(TransformListener listener) throws TransformerFactoryException {
        return createTransformReceiver(listener, new TransformerConfig());
    }

    public TransformReceiver createTransformReceiver(Set<TransformListener> listeners) throws TransformerFactoryException {
        return createTransformReceiver(listeners, new TransformerConfig());
    }

    public TransformReceiver createTransformReceiver(TransformListener listener, TransformerConfig config) throws TransformerFactoryException {
        Set<TransformListener> listeners = new HashSet<TransformListener>();
        listeners.add(listener);
        return createTransformReceiver(listeners, config);
    }

    public TransformReceiver createTransformReceiver(Set<TransformListener> listeners, TransformerConfig config) throws TransformerFactoryException {

        // TODO when there is more than one communicator or core implementation, this
        // has to be more sophisticated
        TransformerCore core = new TransformerCoreDefault(config.getCacheTime());
        TransformCommunicator comm = new TransformCommunicatorRSB("read-only");
        try {
            comm.addTransformListener(core);
            comm.init(config);
        } catch (TransformerException ex) {
            throw new TransformerFactoryException("Can not create Transformer because communicator can not be initialized", ex);
        }

        return new TransformReceiver(core, comm, config);
    }

    public TransformPublisher createTransformPublisher(String name) throws TransformerFactoryException {
        return createTransformPublisher(name, new TransformerConfig());
    }

    public TransformPublisher createTransformPublisher(String name, TransformerConfig config) throws TransformerFactoryException {

        // TODO when there is more than one communicator or core implementation, this
        // has to be more sophisticated
        TransformCommunicator comm = new TransformCommunicatorRSB(name);
        try {
            comm.init(config);
        } catch (TransformerException ex) {
            throw new TransformerFactoryException("Can not create Transformer because communicator can not be initialized", ex);
        }

        return new TransformPublisher(comm, config);
    }
}
