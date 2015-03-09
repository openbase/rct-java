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
	public Transformer createTransformer(String name) throws TransformerFactoryException {
		return createTransformer(name, new TransformerConfig());
	}
	public Transformer createTransformer(String name, TransformerConfig config) throws TransformerFactoryException {
		Set<TransformListener> listeners = new HashSet<TransformListener>();
		return createTransformer(name, listeners, config);
	}
	public Transformer createTransformer(String name, TransformListener listener) throws TransformerFactoryException {
		return createTransformer(name, listener, new TransformerConfig());
	}
	public Transformer createTransformer(String name, Set<TransformListener> listeners) throws TransformerFactoryException {
		return createTransformer(name, listeners, new TransformerConfig());
	}

	public Transformer createTransformer(String name, TransformListener listener, TransformerConfig config) throws TransformerFactoryException {
		Set<TransformListener> listeners = new HashSet<TransformListener>();
		listeners.add(listener);
		return createTransformer(name, listeners, config);
	}
	public Transformer createTransformer(String name, Set<TransformListener> listeners, TransformerConfig config) throws TransformerFactoryException {
		
		// TODO when there is more than one communicator or core implementation, this
		// has to be more sophisticated
		TransformerCore core = new TransformerCoreDefault(config.getCacheTime());
		TransformCommunicator comm = new TransformCommunicatorRSB(name);
		try {
			comm.addTransformListener(core);
			comm.init(config);
		} catch (TransformerException e) {
			throw new TransformerFactoryException("Can not create Transformer because communicator can not be initialized", e);
		}
		
		return new Transformer(core, comm, config);
	}
}
