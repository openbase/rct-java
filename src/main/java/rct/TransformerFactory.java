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
	public Transformer createTransformer() throws TransformerFactoryException {
		return createTransformer(new TransformerConfig());
	}
	public Transformer createTransformer(TransformerConfig config) throws TransformerFactoryException {
		Set<TransformListener> listeners = new HashSet<TransformListener>();
		return createTransformer(listeners, config);
	}
	public Transformer createTransformer(TransformListener listener) throws TransformerFactoryException {
		return createTransformer(listener, new TransformerConfig());
	}
	public Transformer createTransformer(Set<TransformListener> listeners) throws TransformerFactoryException {
		return createTransformer(listeners, new TransformerConfig());
	}

	public Transformer createTransformer(TransformListener listener, TransformerConfig config) throws TransformerFactoryException {
		Set<TransformListener> listeners = new HashSet<TransformListener>();
		listeners.add(listener);
		return createTransformer(listeners, config);
	}
	public Transformer createTransformer(Set<TransformListener> listeners, TransformerConfig config) throws TransformerFactoryException {
		
		// TODO when there is more than one communicator or core implementation, this
		// has to be more sophisticated
		TransformerCore core = new TransformerCoreDefault(config.getCacheTime());
		TransformCommunicator comm = new TransformCommunicatorRSB();
		try {
			comm.addTransformListener(core);
			comm.init(config);
		} catch (TransformerException e) {
			throw new TransformerFactoryException("Can not create Transformer because communicator can not be initialized", e);
		}
		
		return new Transformer(core, comm, config);
	}
}
