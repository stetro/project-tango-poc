package de.stetro.master.masterprototype.rendering.primitives;


import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.rajawali3d.math.vector.Vector3;

public class IntersectionPoints extends MemoryPoints {
    public IntersectionPoints(int numberOfPoints) {
        super(numberOfPoints, 5.0f);
    }

    public boolean intersect(Vector3 startRay, Vector3 endRay) {
        synchronized (this) {
            Line line = new Line(new Vector3D(startRay.x, startRay.y, startRay.z), new Vector3D(endRay.x, endRay.y, endRay.z));
            double minimumDistance = 0.05;
            this.intersection = null;
            currentPoints.position(0);
            for (int i = 0; i < currentPointsCount; i++) {
                float x = currentPoints.get();
                float y = currentPoints.get();
                float z = currentPoints.get();
                Vector3 p = new Vector3(x, y, z);
                p.multiply(getModelMatrix());
                double distance = line.distance(new Vector3D(p.toArray()));
                if (distance <= minimumDistance) {
                    this.intersection = p;
                    return true;
                }
            }
            return false;
        }
    }
}
