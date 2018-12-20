package org.openbase.rct.impl;

import org.openbase.rct.Transform;

public interface TransformListener {

    public void newTransformAvailable(Transform transform, boolean isStatic);
}
