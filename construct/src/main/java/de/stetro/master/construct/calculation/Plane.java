package de.stetro.master.construct.calculation;

import org.rajawali3d.math.vector.Vector3;

public class Plane {

    public final Vector3 normal;
    public final double distance;

    public Plane(Vector3 normal, double distance) {
        this.normal = normal;
        this.distance = distance;
    }

    /**
     * create Hesse Normal Plane from 3 points
     *
     * @param p0 point 1
     * @param p1 point 2
     * @param p2 point 3
     * @return returns plane in following form {nx,ny,nz,d}
     */
    protected static Plane createHessePlane(Vector3 p0, Vector3 p1, Vector3 p2) {
        // Vector3s
        Vector3 a = p1.clone().subtract(p0);
        Vector3 b = p2.clone().subtract(p0);

        // cross product -> normal Vector3
        Vector3 normal = a.cross(b);
        normal.normalize();

        // distance to origin
        Vector3 scale = p0.clone().multiply(normal);
        double distance = scale.x + scale.y + scale.z;

        return new Plane(normal, distance);
    }

    public double distanceTo(Vector3 point) {
        return point.x * normal.x + point.y * normal.y + point.z * normal.z - distance;
    }

    @Override
    public String toString() {
        return "distance: " + distance + " normal: " + String.valueOf(normal);
    }
}

