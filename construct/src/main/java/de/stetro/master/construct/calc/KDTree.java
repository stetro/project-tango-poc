package de.stetro.master.construct.calc;


import android.support.annotation.NonNull;

import com.github.quickhull3d.Point3d;

import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class KDTree {
    private final Vector3 point;
    private final int depth;
    private KDTree left, right;

    public KDTree(List<Vector3> points, Vector3 mean, KDSplit split, int depth) {
        this.depth = depth;
        this.point = mean;
        if (points.size() <= 1) {
            return;
        }
        List<Vector3> rightPoints = new ArrayList<>();
        List<Vector3> leftPoints = new ArrayList<>();
        for (Vector3 point : points) {
            if (point.equals(mean)) {
                continue;
            }
            if (split.comparator.compare(mean, point) > 0) {
                rightPoints.add(point);
            } else {
                leftPoints.add(point);
            }
        }
        split = iterateSplitVariable(split);
        depth++;
        if (rightPoints.size() > 0) {
            right = new KDTree(rightPoints, getMedian(rightPoints, split), split, depth);
        }
        if (leftPoints.size() > 0) {
            left = new KDTree(leftPoints, getMedian(leftPoints, split), split, depth);
        }
    }

    public KDTree(List<Vector3> pointList, Vector3 point) {
        this(pointList, point, KDSplit.X_AXIS, 0);
    }

    public static Vector3 getInitialMedian(List<Vector3> pointList) {
        return getMedian(pointList, KDSplit.X_AXIS);
    }

    private static Vector3 getMedian(List<Vector3> pointList, KDSplit split) {
        double[] components = new double[pointList.size()];
        for (int i = 0; i < pointList.size(); i++) {
            components[i] = split.get(pointList.get(i));
        }
        Arrays.sort(components);
        for (Vector3 point : pointList) {
            if (split.get(point) == components[(components.length - 1) / 2]) {
                return point;
            }
        }
        return pointList.get(0);
    }

    @NonNull
    private KDSplit iterateSplitVariable(KDSplit split) {
        switch (split) {
            case X_AXIS:
                split = KDSplit.Y_AXIS;
                break;
            case Y_AXIS:
                split = KDSplit.Z_AXIS;
                break;
            case Z_AXIS:
                split = KDSplit.X_AXIS;
                break;
        }
        return split;
    }

    public Vector3 getPoint() {
        return point;
    }

    public KDTree getLeft() {
        return left;
    }

    public KDTree getRight() {
        return right;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        String separator = "";
        for (int i = 0; i < depth; i++) {
            separator += "\t";
        }
        b.append(separator).append("point: ").append(point.toString());
        if (right != null) {
            b.append(separator).append(" \nright:").append(right.toString());
        }
        if (left != null) {
            b.append(separator).append(" \nleft:").append(left.toString());
        }
        return b.toString();
    }

    public List<Cluster> getClusters() {
        List<Cluster> clusters = new ArrayList<>();

        return clusters;
    }

    public enum KDSplit {
        X_AXIS(), Y_AXIS(), Z_AXIS();
        public final KDComparator comparator;

        KDSplit() {
            comparator = new KDComparator(this);
        }

        public double get(Vector3 p) {
            switch (this) {
                case X_AXIS:
                    return p.x;
                case Y_AXIS:
                    return p.y;
                default:
                    return p.z;
            }
        }
    }

    private static class KDComparator implements Comparator<Vector3> {
        private KDSplit split;

        public KDComparator(KDSplit split) {
            this.split = split;
        }

        @Override
        public int compare(Vector3 lhs, Vector3 rhs) {
            return Double.compare(split.get(lhs), split.get(rhs));
        }
    }

    public class Cluster {
        private Point3d[] points;

        public Point3d[] getPoints() {
            return points;
        }
    }
}
