package rct.impl;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rct.Transform;

public class TransformRequest {

	static public class FutureTransform implements Future<Transform> {
		
		private Transform transform = null;
		private Object lock = new Object();
		private boolean cancelled = false;

		public boolean cancel(boolean mayInterruptIfRunning) {
			synchronized (lock) {
				cancelled = true;
				if (mayInterruptIfRunning) {
					lock.notify();
					return true;
				}
				return false;
			}
		}
		public void set(Transform t) {
			synchronized (lock) {
				if (!cancelled) {
					transform = t;
					lock.notify();
				}
			}
		}
		public Transform get() throws InterruptedException, ExecutionException {
			synchronized (lock) {
				if (cancelled) {
					throw new CancellationException();
				}
				if (transform != null) {
					return transform;
				} else {
					lock.wait();
					if (cancelled) {
						throw new CancellationException();
					}
					return transform;
				}
			}
		}

		public Transform get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			synchronized (lock) {
				if (cancelled) {
					throw new CancellationException();
				}
				if (transform != null) {
					return transform;
				} else {
					lock.wait(TimeUnit.MILLISECONDS.convert( timeout, unit ));
					if (cancelled) {
						throw new CancellationException();
					}
					if (transform == null) {
						throw new TimeoutException();
					}
					return transform;
				}
			}
		}
		public boolean isCancelled() {
			return cancelled;
		}
		public boolean isDone() {
			return cancelled || transform != null;
		}
	}
	
	public TransformRequest(String target_frame, String source_frame, long time, FutureTransform future) {
		this.target_frame = target_frame;
		this.source_frame = source_frame;
		this.time = time;
		this.future = future;
	}
	
	public String target_frame;
	public String source_frame;
	public long time;
	public FutureTransform future;
}
