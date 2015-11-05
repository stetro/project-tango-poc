package de.stetro.master.ar.calc;

import org.apache.commons.math3.util.FastMath;

import java.util.LinkedList;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

public class RANSAC {
    // list of points supporting the plane computed in detectPlane
    public static LinkedList<float[]> supportingPoints = null;
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
    public static float[] detectPlane(float[][] points, float distanceThresh, int numIterations, int sufficientSupport) {
        // RANSAC
        int numPoints = points.length;
        boolean[] picked = new boolean[numPoints];
        LinkedList<float[]> supportPointsMax = null;
        int supportMax = 0;
        while (numIterations > 0) {
            numIterations--;
            int[] p = new int[3];
            //
            // (1) pick 3 mutually different points ----------------------------
            for (int i = 0; i < 3; i++) {
                do {
                    p[i] = (int) (Math.random() * numPoints);
                } while (picked[p[i]]);
                picked[p[i]] = true;
            }
            picked[p[0]] = false;
            picked[p[1]] = false;
            picked[p[2]] = false;
            //
            // (2) create plane ------------------------------------------------
            // plane is represented in Hesse normal form, n = normal, d = distance
            float[] p0 = points[p[0]];
            float[] p1 = points[p[1]];
            float[] p2 = points[p[2]];
            float[] plane = createPlane(p0, p1, p2); // returns {n0,n1,n2,d}
            //
            // (3) compute support
            computeSupport(plane, points, distanceThresh);
            int support = supportingPoints.size();
            //
            // (4) if support is larger then current best support:
            // use this plane
            if (support > supportMax) {
                supportMax = support;
                supportPointsMax = supportingPoints;
            }
            //
            // if there is already sufficient support, stop iterating.
            if (supportMax >= sufficientSupport) {
                break;
            }
        }
        //
        // use max. set of supporting points to re-compute the plane and support
        // pointlist and flag array (fields) are re-computed.
        // this method returns the plane in hesse form
        float[] plane = planeRegression(supportPointsMax);
        computeSupport(plane, points, distanceThresh);
        return (plane);
    }

    // -------------------------------------------------------------------------
    // returns optimal (=> mean square error)  plane in Hesse Normal Form
    // hnf = {nx,ny,nz,d}
    private static float[] planeRegression(LinkedList<float[]> pts) {
        int numPoints = pts.size();
        // compute mean
        float[] mean = {0, 0, 0};
        for (float[] p : pts) {
            mean[0] += p[0];
            mean[1] += p[1];
            mean[2] += p[2];
        }
        mean[0] /= pts.size();
        mean[1] /= pts.size();
        mean[2] /= pts.size();

        // subtract mean (and create array from list on the fly)
        float[][] points = new float[numPoints][3];
        int i = 0;
        for (float[] p : pts) {
            points[i][0] = p[0] - mean[0];
            points[i][1] = p[1] - mean[1];
            points[i][2] = p[2] - mean[2];
            i++;
        }

        // create covariance matrix
        double[][] COV = new double[3][3];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                for (i = 0; i < numPoints; i++) {
                    COV[r][c] += points[i][r] * points[i][c];
                }
            }
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                COV[r][c] /= numPoints;
            }
        }
        //
        // find smallest Eigenvector
        // EVD
        Matrix K = new Matrix(COV, 3, 3);
        EigenvalueDecomposition evd = new EigenvalueDecomposition(K);
        Matrix V = evd.getV();
        Matrix D = evd.getD();

        // get eigenvector to smallest eigenvalue, that's the normal
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
        float[] hnf = new float[4];
        double nx = V.get(0, smallestIndex);
        double ny = V.get(1, smallestIndex);
        double nz = V.get(2, smallestIndex);
        double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
        hnf[0] = (float) (nx / l);
        hnf[1] = (float) (ny / l);
        hnf[2] = (float) (nz / l);

        // distance: n*mean
        double d = nx * mean[0] + ny * mean[1] + nz * mean[2];
        hnf[3] = (float) d;
        return (hnf);
    }

    /**
     * create Hesse Normal Plane from 3 points
     *
     * @param p0 point 1
     * @param p1 point 2
     * @param p2 point 3
     * @return returns plane in following form {nx,ny,nz,d}
     */
    private static float[] createPlane(float[] p0, float[] p1, float[] p2) {
        // vectors
        float[] a = {p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]};
        float[] b = {p2[0] - p0[0], p2[1] - p0[1], p2[2] - p0[2]};
        // cross product -> normal vector
        float[] n = new float[3];
        n[0] = a[1] * b[2] - a[2] * b[1];
        n[1] = a[2] * b[0] - a[0] * b[2];
        n[2] = a[0] * b[1] - a[1] * b[0];
        float l = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
        n[0] /= l;
        n[1] /= l;
        n[2] /= l;
        // distance to origin
        float d = p0[0] * n[0] + p0[1] * n[1] + p0[2] * n[2];
        return (new float[]{n[0], n[1], n[2], d});
    }

    /**
     * calculates the list of supporting Points for the given 3 points
     *
     * @param plane   found plane in RANSAC iteration
     * @param points  list of all points
     * @param mindist minimum distance between point and plane to determine support of a point
     */
    private static void computeSupport(float[] plane, float[][] points, float mindist) {
        supportingPoints = new LinkedList<>();
        for (float[] point : points) {
            float distanceToPlane = point[0] * plane[0] + point[1] * plane[1] + point[2] * plane[2] - plane[3];
            if (FastMath.abs(distanceToPlane) <= mindist) {
                supportingPoints.addLast(point);
            }
        }
    }
}