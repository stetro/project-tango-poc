package de.stetro.master.construct.rendering;

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

import de.stetro.master.construct.calc.OctTree;
import de.stetro.master.construct.calc.Plane;
import de.stetro.master.construct.calc.RANSAC;
import de.stetro.master.construct.calc.hull.GrahamScan;
import de.stetro.master.construct.calc.hull.Point2D;

public class MeshTree extends OctTree {

    public static final float RANSAC_DISTANCE_THRESH = 0.05f;
    public static final int RANSAC_ITERATIONS = 8;
    public static final double RANSAC_SUPPORT = 0.33;
    private static final String tag = MeshTree.class.getSimpleName();
    private static final int DETECTED_PLANES = 3;
    public static final double SCALE_FACTOR = 0.08;
    private final Vector3 centroid;
    private Plane[] planes = new Plane[DETECTED_PLANES];
    private Stack<Vector3> polygons;
    private List<Vector3> newPoints;
    private int generatorDepth;

    public MeshTree(Vector3 position, double range, int depth, int generatorDepth) {
        super(position, range, depth);
        centroid = new Vector3(position.x + halfRange, position.y + halfRange, position.z + halfRange);
        this.generatorDepth = generatorDepth;
        if (depth == generatorDepth) {
            polygons = new Stack<>();
            newPoints = new ArrayList<>();
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

    public void putPoints(Vector3 randomPoint, Vector3[] points) {
        if (depth == generatorDepth) {
            for (Vector3 point : points) {
                if (inside(point)) {
                    Vector3 scaled = scale(point, centroid, SCALE_FACTOR);
                    Plane plane = getSupportedPlane(scaled);
                    if (plane == null) {
                        this.newPoints.add(scaled);
                    } else {
                        plane.addPoint(scaled);
                    }
                }
            }
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

    private Plane getSupportedPlane(Vector3 point) {
        for (Plane plane : planes) {
            if (plane != null && plane.distanceTo(point) < RANSAC_DISTANCE_THRESH) {
                return plane;
            }
        }
        return null;
    }

    private Vector3 scale(Vector3 point, Vector3 centroid, double factor) {
        Vector3 clone = point.clone();
        Vector3 centered = clone.subtract(centroid);
        Vector3 scaled = centered.multiply(1.0 + factor);
        return scaled.add(centroid);
    }

    private void putPoints(Vector3 randomPoint, Vector3[] points, int clusterIndex, double x, double y, double z) {
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
        for (int i = 0; i < DETECTED_PLANES; i++) {

            // skip if size of new Points is to small and there is not already a plane
            if (newPoints.size() < 3 && planes[i] == null) {
                continue;
            }

            List<Vector3> relevantPoints;
            if (planes[i] == null) {
                int sufficientSupport = (int) (newPoints.size() * RANSAC_SUPPORT);

                // detect plane in hesse normal form
                planes[i] = RANSAC.detectPlane(newPoints, RANSAC_DISTANCE_THRESH, RANSAC_ITERATIONS, sufficientSupport);

                // skip plane if not sufficient support by newPoints
                if (RANSAC.supportingPoints.size() < 4 || RANSAC.supportingPoints.size() < sufficientSupport) {
                    planes[i] = null;
                    continue;
                }
                newPoints = RANSAC.notSupportingPoints;
                relevantPoints = RANSAC.supportingPoints;
            } else {
                relevantPoints = planes[i].getPoints();
            }


            // calculate convex hull and vertices for supporting newPoints with GrahamScan
            Point2D[] innerHullPoints = new Point2D[relevantPoints.size()];
            for (int j = 0; j < relevantPoints.size(); j++) {
                innerHullPoints[j] = planes[i].transferTo2D(relevantPoints.get(j));
            }
            GrahamScan scan = new GrahamScan(innerHullPoints);
            // create DelaunayTriangle from convex hull
            List<TriangulationPoint> hullPoints = new ArrayList<>();
            Stack<Point2D> hull = scan.hull();
            planes[i].getPoints().clear();
            for (Point2D point2D : hull) {
                Vector3 vector3 = planes[i].transferTo3D(point2D);
                planes[i].addPoint(vector3);
                hullPoints.add(new TPoint(vector3.x, vector3.y, vector3.z));
            }
            PointSet ps = new PointSet(hullPoints);
            try {
                Poly2Tri.triangulate(ps);
                for (DelaunayTriangle delaunayTriangle : ps.getTriangles()) {
                    polygons.add(new Vector3(delaunayTriangle.points[0].getX(), delaunayTriangle.points[0].getY(), delaunayTriangle.points[0].getZ()));
                    polygons.add(new Vector3(delaunayTriangle.points[1].getX(), delaunayTriangle.points[1].getY(), delaunayTriangle.points[1].getZ()));
                    polygons.add(new Vector3(delaunayTriangle.points[2].getX(), delaunayTriangle.points[2].getY(), delaunayTriangle.points[2].getZ()));
                }
            } catch (NullPointerException ignored) {
                Log.e(tag, "failed with triangulate with " + hull.size() + " hull points");
            }
        }
        newPoints.clear();
    }

    public void clear() {
        if (depth == generatorDepth) {
            polygons.clear();
            newPoints.clear();
            planes = new Plane[DETECTED_PLANES];
        } else {
            for (OctTree child : children) {
                if (child != null) {
                    child.clear();
                }
            }
        }
    }

    public void putPoints(List<Vector3> points) {
        for (Vector3 point : points) {
            putPoints(point);
        }
    }

    private void putPoints(Vector3 point) {
        if (depth == generatorDepth) {
            Vector3 scaled = scale(point, centroid, SCALE_FACTOR);
            this.newPoints.add(scaled);
        } else {
            if (point.x < position.x + halfRange) {
                if (point.y < position.y + halfRange) {
                    if (point.z < position.z + halfRange) {
                        putPoints(point, 0, position.x, position.y, position.z);
                    } else {
                        putPoints(point, 1, position.x, position.y, position.z + halfRange);
                    }
                } else {
                    if (point.z < position.z + halfRange) {
                        putPoints(point, 2, position.x, position.y + halfRange, position.z);
                    } else {
                        putPoints(point, 3, position.x, position.y + halfRange, position.z + halfRange);
                    }
                }
            } else {
                if (point.y < position.y + halfRange) {
                    if (point.z < position.z + halfRange) {
                        putPoints(point, 4, position.x + halfRange, position.y, position.z);
                    } else {
                        putPoints(point, 5, position.x + halfRange, position.y, position.z + halfRange);
                    }
                } else {
                    if (point.z < position.z + halfRange) {
                        putPoints(point, 6, position.x + halfRange, position.y + halfRange, position.z);
                    } else {
                        putPoints(point, 7, position.x + halfRange, position.y + halfRange, position.z + halfRange);
                    }
                }
            }
        }
    }

    private void putPoints(Vector3 point, int clusterIndex, double x, double y, double z) {
        if (children[clusterIndex] == null) {
            children[clusterIndex] = new MeshTree(new Vector3(x, y, z), halfRange, depth - 1, generatorDepth);
        }
        ((MeshTree) children[clusterIndex]).putPoints(point);
    }

    public int getNewPointsCount() {
        if (depth == generatorDepth) {
            return newPoints.size();
        } else {
            int count = 0;
            for (OctTree child : children) {
                if (child != null) {
                    count += ((MeshTree) child).getNewPointsCount();
                }
            }
            return count;
        }
    }
}
