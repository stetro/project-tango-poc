package de.stetro.master.pc.util;


import android.os.AsyncTask;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;

import org.rajawali3d.math.vector.Vector3;

import java.util.List;
import java.util.Stack;

import de.stetro.master.pc.R;
import de.stetro.master.pc.calc.Cluster;
import de.stetro.master.pc.calc.KMeans;
import de.stetro.master.pc.calc.OctTree;
import de.stetro.master.pc.calc.RANSAC;
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
                .title(R.string.exporting_pointcloud)
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

        @Override
        protected Stack<Vector3> doInBackground(PointCollection... params) {
            if (params.length == 0) {
                return null;
            }

            PointCollection pointCollection = params[0];
            List<OctTree> cluster = pointCollection.getOctTree().getCluster(7);
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
                if (points.size() < 10) {
                    break;
                }

                // 30% of left points need to be supported
                int sufficientSupport = (int) (points.size() * 0.30);

                // detect plane in hesse normal form
                RANSAC.HessePlane hessePlane = RANSAC.detectPlane(points, 0.05f, 10, sufficientSupport);
                Log.d(tag, "Found potential Plane :" + hessePlane.toString());
                points = RANSAC.notSupportingPoints;

                // skip plane if not sufficient support by points
                if (RANSAC.supportingPoints.size() < sufficientSupport || RANSAC.supportingPoints.size() < 4) {
                    continue;
                }

                // find cluster in selected Points
                KMeans kMeans = new KMeans();
                kMeans.init(RANSAC.supportingPoints);
                kMeans.calculate();
                List<Cluster> clusters = kMeans.getClusters();

                Log.d(tag, "found " + clusters.size() + " clusters in a plane with " + RANSAC.supportingPoints.size() + " points");
                // calculate convex hull and vertices for supporting points
                for (Cluster c : clusters) {
                    Log.d(tag, "iterate over cluster with " + c.getPoints().size() + " points");
                    if (c.getPoints().size() < 5) {
                        continue;
                    }
                    Point3d[] innerHullPoints = new Point3d[c.getPoints().size()];

                    for (int j = 0; j < c.getPoints().size(); j++) {
                        Vector3 point = c.getPoints().get(j).clone();
                        innerHullPoints[j] = new Point3d(point.x, point.y, point.z);
                    }

                    QuickHull3D hull = new QuickHull3D();
                    hull.build(innerHullPoints);
                    Point3d[] hullVertices = hull.getVertices();
                    int[][] faces = hull.getFaces();

                    for (int[] face : faces) {
                        stack.add(new Vector3(hullVertices[face[0]].x, hullVertices[face[0]].y, hullVertices[face[0]].z));
                        stack.add(new Vector3(hullVertices[face[1]].x, hullVertices[face[1]].y, hullVertices[face[1]].z));
                        stack.add(new Vector3(hullVertices[face[2]].x, hullVertices[face[2]].y, hullVertices[face[2]].z));
                    }
                }
            }
        }

    }
}
