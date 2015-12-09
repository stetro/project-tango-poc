package de.stetro.master.pc.util;


import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

import org.poly2tri.Poly2Tri;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.point.TPoint;
import org.poly2tri.triangulation.sets.PointSet;
import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.stetro.master.pc.R;
import de.stetro.master.pc.calc.Cluster;
import de.stetro.master.pc.calc.KMeans;
import de.stetro.master.pc.calc.OctTree;
import de.stetro.master.pc.calc.Plane;
import de.stetro.master.pc.calc.RANSAC;
import de.stetro.master.pc.calc.hull.GrahamScan;
import de.stetro.master.pc.calc.hull.Point2D;
import de.stetro.master.pc.rendering.PointCloudARRenderer;
import de.stetro.master.pc.rendering.PointCollection;
import de.stetro.master.pc.ui.MainActivity;

public class ReconstructionBuilder {
    private final static String tag = ReconstructionBuilder.class.getSimpleName();
    private final PointCollection collectedPoints;
    private final PointCloudARRenderer renderer;
    private MaterialDialog dialog;

    public ReconstructionBuilder(MainActivity context, PointCollection collectedPoints, PointCloudARRenderer renderer) {
        this.collectedPoints = collectedPoints;
        this.renderer = renderer;
        dialog = new MaterialDialog.Builder(context)
                .title(R.string.calculating_mesh)
                .content(R.string.please_wait)
                .progress(false, 100, true)
                .widgetColor(context.getResources().getColor(R.color.colorPrimary))
                .build();
        dialog.setProgress(0);
    }

    public void reconstruct() {
        new ReconstructionAsyncTask().execute(collectedPoints);
    }


    private class ReconstructionAsyncTask extends AsyncTask<PointCollection, Integer, Stack<Vector3>> {

        @SuppressWarnings("UnnecessaryLocalVariable")
        @Override
        protected Stack<Vector3> doInBackground(PointCollection... params) {
            if (params.length == 0) {
                return null;
            }

            PointCollection pointCollection = params[0];
            Stack<Vector3> stack = createPlanarReconstruction(pointCollection);
//            Stack<Vector3> stack = createMarchingCubeReconstruction(pointCollection);
            return stack;
        }

//        private Stack<Vector3> createMarchingCubeReconstruction(PointCollection pointCollection) {
//            Stack<Vector3> stack = new Stack<>();
//            List<Cube> cubes = pointCollection.getMeshTree().getCubes(0);
//            Log.d(tag, "found " + cubes.size() + " cubes");
//            for (Cube c : cubes) {
//                c.getFaces(stack);
//            }
//            Log.d(tag, "got " + stack.size() / 3 + " faces");
//            return stack;
//        }

        @NonNull
        private Stack<Vector3> createPlanarReconstruction(PointCollection pointCollection) {
            List<OctTree> cluster = pointCollection.getMeshTree().getCluster(7);
            Log.d(tag, "found " + cluster.size() + " spartial clusters inside octtree");
            dialog.setMaxProgress(cluster.size());

            Log.d(tag, "moving points to listable data structure");
            Stack<Vector3> stack = new Stack<>();
            for (int i = 0; i < cluster.size(); i++) {
                dialog.setProgress(i);
                List<Vector3> points = cluster.get(i).getPointList();
                detectPlanesAndGeneratePolygons(stack, points);
            }
            return stack;
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected void onPostExecute(Stack<Vector3> points) {
            dialog.dismiss();
            if (points != null) {
                renderer.setFaces(points);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            dialog.setProgress(values[0]);
        }

        private void detectPlanesAndGeneratePolygons(Stack<Vector3> stack, List<Vector3> points) {
            for (int i = 0; i < 3; i++) {

                // skip iterating when enough points are matched
                if (points.size() < 4) {
                    break;
                }

                // 30% of left points need to be supported
                int sufficientSupport = (int) (points.size() * 0.30);

                // detect plane in hesse normal form
                Plane hessePlane = RANSAC.detectPlane(points, 0.06f, 10, sufficientSupport);
                Log.d(tag, "Found potential Plane :" + hessePlane.toString());
                points = RANSAC.notSupportingPoints;

                // skip plane if not sufficient support by points
                if (RANSAC.supportingPoints.size() < sufficientSupport || RANSAC.supportingPoints.size() < 4) {
                    continue;
                }

                // find cluster in selected Points
                KMeans kMeans = new KMeans();
                kMeans.init(RANSAC.supportingPoints, 3);
                kMeans.calculate();
                List<Cluster> clusters = kMeans.getClusters();

                Log.d(tag, "found " + clusters.size() + " clusters in a plane with " + RANSAC.supportingPoints.size() + " points");

                for (Cluster c : clusters) {
                    Log.d(tag, "iterate over cluster with " + c.getPoints().size() + " points");
                    if (c.getPoints().size() < 5) {
                        continue;
                    }
                    // calculate convex hull and vertices for supporting points with GrahamScan
                    Point2D[] innerHullPoints = new Point2D[c.getPoints().size()];
                    for (int j = 0; j < c.getPoints().size(); j++) {
                        innerHullPoints[j] = hessePlane.transferTo2D(c.getPoints().get(j));
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
                        stack.add(new Vector3(delaunayTriangle.points[0].getX(), delaunayTriangle.points[0].getY(), delaunayTriangle.points[0].getZ()));
                        stack.add(new Vector3(delaunayTriangle.points[1].getX(), delaunayTriangle.points[1].getY(), delaunayTriangle.points[1].getZ()));
                        stack.add(new Vector3(delaunayTriangle.points[2].getX(), delaunayTriangle.points[2].getY(), delaunayTriangle.points[2].getZ()));
                    }
                }
            }
        }
    }
}
