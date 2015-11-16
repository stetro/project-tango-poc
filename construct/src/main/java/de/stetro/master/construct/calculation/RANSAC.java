package de.stetro.master.construct.calculation;

import org.apache.commons.math3.util.FastMath;
import org.rajawali3d.math.vector.Vector3;

import java.util.LinkedList;
import java.util.List;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class RANSAC {
    // list of points supporting the plane computed in detectPlane
    public static LinkedList<Vector3> supportingPoints = null;
    public static LinkedList<Vector3> notSupportingPoints = null;
    // array of boolean: if supportFlag[i] == true, points[i] is a supporting point

    /**
     * detects a plane inside a pointcloud
     *
     * @param points            in 3d space of type and size float[n][3]
     * @param distanceThresh    plane distance for accepted iterations
     * @param numIterations     number of RANSAC iterations
     * @param sufficientSupport stop criteria for sufficient plane support
     * @return a plane defined in hesse normal form
     */
    public static HessePlane detectPlane(List<Vector3> points, float distanceThresh, int numIterations, int sufficientSupport) {
        int pointCount = points.size();
        boolean[] picked = new boolean[pointCount];
        LinkedList<Vector3> supportPointsMax = new LinkedList<>();
        int supportMax = 0;

        while (numIterations > 0) {
            numIterations--;

            // (1) pick 3 mutually different points ----------------------------
            int[] randomPointIndices = new int[3];
            for (int i = 0; i < 3; i++) {
                do {
                    randomPointIndices[i] = (int) (Math.random() * pointCount);
                } while (picked[randomPointIndices[i]]);
                picked[randomPointIndices[i]] = true;
            }
            picked[randomPointIndices[0]] = false;
            picked[randomPointIndices[1]] = false;
            picked[randomPointIndices[2]] = false;

            // (2) create plane ------------------------------------------------
            // plane is represented in Hesse normal form, n = normal, d = distance


            Vector3 p0 = points.get(randomPointIndices[0]);
            Vector3 p1 = points.get(randomPointIndices[1]);
            Vector3 p2 = points.get(randomPointIndices[2]);
            HessePlane plane = HessePlane.createHessePlane(p0, p1, p2); // returns {n0,n1,n2,d}

            // (3) compute support
            computeSupport(plane, points, distanceThresh);
            int support = supportingPoints.size();

            // (4) if support is larger then current best support:
            // use this plane
            if (support > supportMax) {
                supportMax = support;
                supportPointsMax = supportingPoints;
            }

            // if there is already sufficient support, stop iterating.
            if (supportMax >= sufficientSupport) {
                break;
            }
        }

        // use max. set of supporting points to re-compute the plane and support
        // pointlist and flag array (fields) are re-computed.
        // this method returns the plane in hesse form
        HessePlane plane = planeRegression(supportPointsMax);
        computeSupport(plane, points, distanceThresh);
        return (plane);
    }

    // -------------------------------------------------------------------------
    // returns optimal (=> mean square error)  plane in Hesse Normal Form
    // hnf = {nx,ny,nz,d}
    private static HessePlane planeRegression(LinkedList<Vector3> pts) {
        int numPoints = pts.size();
        // compute mean
        Vector3 mean = new Vector3();
        for (Vector3 p : pts) {
            mean.add(p);
        }
        mean.divide(pts.size());

        // subtract mean (and create array from list on the fly)
        LinkedList<Vector3> points = new LinkedList<>();
        int i = 0;
        for (Vector3 p : pts) {
            points.add(p.clone().subtract(mean));
            i++;
        }

        // create covariance matrix
        double[][] COV = new double[3][3];
        for (Vector3 p : points) {
            COV[0][0] += p.x * p.x;
            COV[0][1] += p.x * p.y;
            COV[0][2] += p.x * p.z;
            COV[1][0] += p.y * p.x;
            COV[1][1] += p.y * p.y;
            COV[1][2] += p.y * p.z;
            COV[2][0] += p.z * p.x;
            COV[2][1] += p.z * p.y;
            COV[2][2] += p.z * p.z;

        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                COV[r][c] /= numPoints;
            }
        }

        // find smallest EigenVector3
        // EVD
        Matrix K = new Matrix(COV, 3, 3);
        EigenvalueDecomposition evd = new EigenvalueDecomposition(K);
        Matrix V = evd.getV();
        Matrix D = evd.getD();

        // get eigenVector3 to smallest eigenvalue, that's the normal
        int smallestIndex = 2;
        double smallestEV = D.get(2, 2);
        if (D.get(1, 1) < smallestEV) {
            smallestEV = D.get(1, 1);
            smallestIndex = 1;
        }
        if (D.get(0, 0) < smallestEV) {
            smallestEV = D.get(0, 0);
            smallestIndex = 0;
        }
        double nx = V.get(0, smallestIndex);
        double ny = V.get(1, smallestIndex);
        double nz = V.get(2, smallestIndex);

        Vector3 normal = new Vector3(nx, ny, nz);
        normal.normalize();

        // distance: n*mean
        double d = nx * mean.x + ny * mean.y + nz * mean.z;
        return new HessePlane(normal, d);
    }


    /**
     * calculates the list of supporting Points for the given 3 points
     *
     * @param plane           found plane in RANSAC iteration
     * @param points          list of all points
     * @param minimumDistance minimum distance between point and plane to determine support of a point
     */
    private static void computeSupport(HessePlane plane, List<Vector3> points, double minimumDistance) {
        supportingPoints = new LinkedList<>();
        notSupportingPoints = new LinkedList<>();
        for (Vector3 point : points) {
            double distanceToPlane = plane.distanceTo(point);
            if (FastMath.abs(distanceToPlane) <= minimumDistance) {
                supportingPoints.addLast(point);
            } else {
                notSupportingPoints.add(point);
            }
        }
    }

    public static class HessePlane {
        public final Vector3 normal;
        public final double distance;

        public HessePlane(Vector3 normal, double distance) {
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
        private static HessePlane createHessePlane(Vector3 p0, Vector3 p1, Vector3 p2) {
            // Vector3s
            Vector3 a = p1.clone().subtract(p0);
            Vector3 b = p2.clone().subtract(p0);

            // cross product -> normal Vector3
            Vector3 normal = a.cross(b);
            normal.normalize();

            // distance to origin
            Vector3 scale = p0.clone().multiply(normal);
            double distance = scale.x + scale.y + scale.z;

            return new HessePlane(normal, distance);
        }

        public double distanceTo(Vector3 point) {
            return point.x * normal.x + point.y * normal.y + point.z * normal.z - distance;
        }

        @Override
        public String toString() {
            return "distance: " + distance + " normal: " + String.valueOf(normal);
        }
    }
}