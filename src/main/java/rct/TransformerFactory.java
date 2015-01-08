package rct;

import java.util.HashSet;
import java.util.Set;

import rct.impl.TransformCommunicator;
import rct.impl.TransformListener;
import rct.impl.TransformerCore;
import rct.impl.TransformerCoreDefault;
import rct.impl.rsb.TransformCommunicatorRSB;

public class TransformerFactory {
	public Transformer createTransformer() {
		return createTransformer(new TransformerConfig());
	}
	public Transformer createTransformer(TransformerConfig config) {
		Set<TransformListener> listeners = new HashSet<TransformListener>();
		return createTransformer(listeners, config);
	}
	public Transformer createTransformer(TransformListener listener) {
		return createTransformer(listener, new TransformerConfig());
	}
	public Transformer createTransformer(Set<TransformListener> listeners) {
		return createTransformer(listeners, new TransformerConfig());
	}

	public Transformer createTransformer(TransformListener listener, TransformerConfig config) {
		Set<TransformListener> listeners = new HashSet<TransformListener>();
		listeners.add(listener);
		return createTransformer(listeners, config);
	}
	public Transformer createTransformer(Set<TransformListener> listeners, TransformerConfig config) {
		
		// TODO when there is more than one communicator or core implementation, this
		// has to be more sophisticated
		TransformerCore core = new TransformerCoreDefault(config.getCacheTime());
		TransformCommunicator comm = new TransformCommunicatorRSB();
		
		return new Transformer(core, comm, config);
	}
}
