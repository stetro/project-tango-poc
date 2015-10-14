package de.stetro.master.masterprototype.calc;


import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.rajawali3d.math.vector.Vector3;

public class Intersection {
    private static final double EPSILON = 0.00001;

    public static boolean isPlaneBetweenPoints(Vector3 planeNormal, Vector3 planePosition, Vector3 cubePosition, Vector3 cameraPosition) {
        Vector3D planePosition3D = new Vector3D(planePosition.x, planePosition.y, planePosition.z);
        Vector3D planeNormal3D = new Vector3D(planeNormal.x, planeNormal.y, planeNormal.z);

        Vector3D a = new Vector3D(cubePosition.x, cubePosition.y, cubePosition.z);
        Vector3D b = new Vector3D(cameraPosition.x, cameraPosition.y, cameraPosition.z);

        Plane plane = new Plane(planePosition3D, planeNormal3D);
        Line line = new Line(a, b);

        Vector3D c = plane.intersection(line);
        if (c == null) {
            return false;
        }
        double crossproduct = b.subtract(a).crossProduct(c.subtract(a)).distance(Vector3D.ZERO);
        if (Math.abs(crossproduct) > EPSILON) {
            return false;
        }
        double dotproduct = b.subtract(a).dotProduct(c.subtract(a));
        if (dotproduct < 0) {
            return false;
        }
        if (dotproduct > a.distanceSq(b)) {
            return false;
        }
        return true;
    }
}
