package de.stetro.master.construct.calc;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

import de.stetro.master.construct.calc.hull.Point2D;

public class Plane {
    private final static String tag = Plane.class.getSimpleName();
    public final Vector3 normal;
    public final double distance;
    private final Vector3 planeOrigin;
    private final Quaternion planeZRotation;
    private final Quaternion inversePlaneZRotation;
    private List<Vector3> points = new ArrayList<>();

    public Plane(Vector3 normal, double distance) {
        this.normal = normal;
        this.distance = distance;
        this.planeOrigin = normal.clone().multiply(distance);
        this.planeZRotation = normal.clone().getRotationTo(new Vector3(0, 0, 1));
        this.inversePlaneZRotation = planeZRotation.clone().inverse();
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

    public Point2D transferTo2D(Vector3 point) {
        point = point.clone();
        point.subtract(planeOrigin);
        point.rotateBy(planeZRotation);
        return new Point2D(point.x, point.y);
    }

    public Vector3 transferTo3D(Point2D point) {
        Vector3 newPoint = new Vector3(point.x(), point.y(), 0);
        newPoint.rotateBy(inversePlaneZRotation);
        newPoint.add(planeOrigin);
        return newPoint;
    }

    public void addPoint(Vector3 point) {
        points.add(point);
    }

    public List<Vector3> getPoints() {
        return points;
    }

}
