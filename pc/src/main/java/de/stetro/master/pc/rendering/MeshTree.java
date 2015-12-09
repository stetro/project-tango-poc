package de.stetro.master.pc.rendering;

import android.util.Log;

import org.poly2tri.Poly2Tri;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.point.TPoint;
import org.poly2tri.triangulation.sets.PointSet;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.stetro.master.pc.calc.OctTree;
import de.stetro.master.pc.calc.Plane;
import de.stetro.master.pc.calc.RANSAC;
import de.stetro.master.pc.calc.hull.GrahamScan;
import de.stetro.master.pc.calc.hull.Point2D;

public class MeshTree extends OctTree {

    private static final String tag = MeshTree.class.getSimpleName();
    private Stack<Vector3> polygons;
    private List<Vector3> points;
    private int generatorDepth;

    public MeshTree(Vector3 position, double range, int depth, int generatorDepth) {
        super(position, range, depth);
        this.generatorDepth = generatorDepth;
        if (depth == generatorDepth) {
            polygons = new Stack<>();
            points = new ArrayList<>();
        }
    }

    public void fillPolygons(Stack<Vector3> polygons) {
        if (this.polygons != null) {
            polygons.addAll(this.polygons);
        } else {
            for (OctTree octTree : getChildren()) {
                MeshTree child = (MeshTree) octTree;
                if (child != null) {
                    child.fillPolygons(polygons);
                }
            }
        }
    }

    public void putPoints(Vector3 randomPoint, List<Vector3> points) {
        if (depth == generatorDepth) {
            this.points.clear();
            for (Vector3 point : points) {
                if (inside(point)) {
                    this.points.add(point);
                }
            }
            updateMesh();
        } else if (randomPoint.x < position.x || randomPoint.x > position.x + range || randomPoint.y < position.y || randomPoint.y > position.y + range || randomPoint.z < position.z || randomPoint.z > position.z + range) {
            return;
//            throw new IllegalArgumentException("Outside of range: " + randomPoint.toString() + " does not belong to " + position.toString() + " with range " + range);
        } else {
            if (randomPoint.x < position.x + halfRange) {
                if (randomPoint.y < position.y + halfRange) {
                    if (randomPoint.z < position.z + halfRange) {
                        putPoints(randomPoint, points, 0, position.x, position.y, position.z);
                    } else {
                        putPoints(randomPoint, points, 1, position.x, position.y, position.z + halfRange);
                    }
                } else {
                    if (randomPoint.z < position.z + halfRange) {
                        putPoints(randomPoint, points, 2, position.x, position.y + halfRange, position.z);
                    } else {
                        putPoints(randomPoint, points, 3, position.x, position.y + halfRange, position.z + halfRange);
                    }
                }
            } else {
                if (randomPoint.y < position.y + halfRange) {
                    if (randomPoint.z < position.z + halfRange) {
                        putPoints(randomPoint, points, 4, position.x + halfRange, position.y, position.z);
                    } else {
                        putPoints(randomPoint, points, 5, position.x + halfRange, position.y, position.z + halfRange);
                    }
                } else {
                    if (randomPoint.z < position.z + halfRange) {
                        putPoints(randomPoint, points, 6, position.x + halfRange, position.y + halfRange, position.z);
                    } else {
                        putPoints(randomPoint, points, 7, position.x + halfRange, position.y + halfRange, position.z + halfRange);
                    }
                }
            }
        }
    }

    private void putPoints(Vector3 randomPoint, List<Vector3> points, int clusterIndex, double x, double y, double z) {
        if (children[clusterIndex] == null) {
            children[clusterIndex] = new MeshTree(new Vector3(x, y, z), halfRange, depth - 1, generatorDepth);
        }
        ((MeshTree) children[clusterIndex]).putPoints(randomPoint, points);
    }

    public void updateMesh() {
        if (depth == generatorDepth) {
            calculatePolygons();
        } else {
            for (OctTree child : children) {
                if (child != null) {
                    ((MeshTree) child).updateMesh();
                }
            }
        }
    }

    private void calculatePolygons() {
        polygons.clear();

        // skip iterating when enough points are matched
        if (points.size() < 4) {
            return;
        }

        // 30% of left points need to be supported
        int sufficientSupport = (int) (points.size() * 0.30);

        // detect plane in hesse normal form
        Plane hessePlane = RANSAC.detectPlane(points, 0.06f, 10, sufficientSupport);
        Log.d(tag, "Found potential Plane :" + hessePlane.toString());
        points = RANSAC.notSupportingPoints;

        // skip plane if not sufficient support by points
        if (RANSAC.supportingPoints.size() < sufficientSupport || RANSAC.supportingPoints.size() < 4) {
            return;
        }

        // calculate convex hull and vertices for supporting points with GrahamScan
        Point2D[] innerHullPoints = new Point2D[RANSAC.supportingPoints.size()];
        for (int j = 0; j < RANSAC.supportingPoints.size(); j++) {
            innerHullPoints[j] = hessePlane.transferTo2D(RANSAC.supportingPoints.get(j));
        }
        GrahamScan scan = new GrahamScan(innerHullPoints);

        // create DelaunayTriangle from convex hull
        List<TriangulationPoint> hullPoints = new ArrayList<>();
        for (Point2D point2D : scan.hull()) {
            Vector3 vector3 = hessePlane.transferTo3D(point2D);
            hullPoints.add(new TPoint(vector3.x, vector3.y, vector3.z));
        }
        PointSet ps = new PointSet(hullPoints);
        Poly2Tri.triangulate(ps);
        for (DelaunayTriangle delaunayTriangle : ps.getTriangles()) {
            polygons.add(new Vector3(delaunayTriangle.points[0].getX(), delaunayTriangle.points[0].getY(), delaunayTriangle.points[0].getZ()));
            polygons.add(new Vector3(delaunayTriangle.points[1].getX(), delaunayTriangle.points[1].getY(), delaunayTriangle.points[1].getZ()));
            polygons.add(new Vector3(delaunayTriangle.points[2].getX(), delaunayTriangle.points[2].getY(), delaunayTriangle.points[2].getZ()));
        }

    }
}
