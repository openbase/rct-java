package rct.impl;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rct.Transform;

public class TransformRequest {

    public String target_frame;
    public String source_frame;
    public long time;
    public FutureTransform future;

    public TransformRequest(String targetFrame, String sourceFrame, long time, FutureTransform future) {
        this.target_frame = targetFrame;
        this.source_frame = sourceFrame;
        this.time = time;
        this.future = future;
    }

    static public class FutureTransform implements Future<Transform> {

        private Transform transform = null;
        private final Object lock = new Object();
        private boolean cancelled = false;

        @Override
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

        @Override
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

        @Override
        public Transform get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            synchronized (lock) {
                if (cancelled) {
                    throw new CancellationException();
                }
                if (transform != null) {
                    return transform;
                } else {
                    lock.wait(TimeUnit.MILLISECONDS.convert(timeout, unit));
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

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return cancelled || transform != null;
        }
    }
}
