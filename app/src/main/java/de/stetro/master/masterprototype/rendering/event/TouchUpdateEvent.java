package de.stetro.master.masterprototype.rendering.event;

import org.rajawali3d.math.vector.Vector3;

public class TouchUpdateEvent {
    private float touchX;
    private float touchY;
    private Vector3 nearIntersectionRayPoint;
    private Vector3 farIntersectionRayPoint;
    private Vector3 intersectionPoint;

    public void setTouchPosition(float x, float y) {
        this.touchX = x;
        this.touchY = y;
    }

    public void setIntersectionRay(Vector3 pointNear, Vector3 pointFar) {
        this.nearIntersectionRayPoint = pointNear;
        this.farIntersectionRayPoint = pointFar;
    }

    public void setIntersectionPoint(Vector3 intersectionPoint) {
        this.intersectionPoint = intersectionPoint;
    }

    public float getTouchX() {
        return touchX;
    }

    public float getTouchY() {
        return touchY;
    }

    public Vector3 getFarIntersectionRayPoint() {
        return farIntersectionRayPoint;
    }

    public Vector3 getIntersectionPoint() {
        return intersectionPoint;
    }

    public Vector3 getNearIntersectionRayPoint() {
        return nearIntersectionRayPoint;
    }
}
