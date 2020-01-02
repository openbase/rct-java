package org.openbase.rct.impl;

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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openbase.rct.Transform;

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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + source_frame + " -> " + target_frame + " at " + time + " solved[" + future.isDone() + "]]";
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
