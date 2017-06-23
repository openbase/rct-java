package rct.impl;

import rct.Transform;

public interface TransformListener {

    public void newTransformAvailable(Transform transform, boolean isStatic);
}
