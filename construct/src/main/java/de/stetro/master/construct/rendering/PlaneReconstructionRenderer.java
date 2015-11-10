package de.stetro.master.construct.rendering;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;
import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.ar.TangoRajawaliRenderer;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.stetro.master.construct.calc.Cluster;
import de.stetro.master.construct.calc.KMeans;
import de.stetro.master.construct.calc.RANSAC;
import de.stetro.master.construct.util.PointCloudManager;

public class PlaneReconstructionRenderer extends TangoRajawaliRenderer {
    private static final int MAX_POINTS = 100000;
    private static final String tag = PlaneReconstructionRenderer.class.getSimpleName();
    private final PointCloudManager pointCloudManager;
    private boolean willCalculatePlanes;
    private Points polygonPoints;
    private Polygon polygon;
    private boolean shouldUpdatePolygon = false;
    private Stack<Vector3> newHullVertices;
    private ArrayList<Vector3> supportingPoints;

    public PlaneReconstructionRenderer(Context context, PointCloudManager pointCloudManager) {
        super(context);
        this.pointCloudManager = pointCloudManager;
    }


    @Override
    protected void initScene() {
        super.initScene();

        Points currentPoints = new Points(MAX_POINTS);
        currentPoints.setMaterial(Materials.getGreenPointCloudMaterial());
        getCurrentScene().addChild(currentPoints);

        polygonPoints = new Points(MAX_POINTS);
        polygonPoints.setMaterial(Materials.getRedPointCloudMaterial());
        getCurrentScene().addChild(polygonPoints);

    }

    public void capturePoints() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (pointCloudManager) {
                    Pose pose = mScenePoseCalcuator.toOpenGLPointCloudPose(pointCloudManager.getDevicePoseAtCloudTime());
                    calculatePlanes(pose);
                }
            }
        }).start();
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    @Override
    protected void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);
        if (pointCloudManager != null && pointCloudManager.hasNewPoints()) {
            synchronized (pointCloudManager) {
                if (shouldUpdatePolygon) {
                    shouldUpdatePolygon = false;
                    if (polygon != null) {
                        getCurrentScene().removeChild(polygon);
                    }
                    polygon = new Polygon(newHullVertices);
                    polygon.setMaterial(Materials.getRedMaterial());
                    polygon.setTransparent(true);
                    getCurrentScene().addChild(polygon);

                    FloatBuffer f = FloatBuffer.allocate(supportingPoints.size() * 3);
                    for (int i = 0; i < supportingPoints.size(); i++) {
                        f.put((float) supportingPoints.get(i).x);
                        f.put((float) supportingPoints.get(i).y);
                        f.put((float) supportingPoints.get(i).z);
                    }

                    Pose pose = mScenePoseCalcuator.toOpenGLPointCloudPose(pointCloudManager.getDevicePoseAtCloudTime());
                    polygonPoints.updatePoints(f, supportingPoints.size());
                    polygonPoints.setPosition(pose.getPosition());
                    polygonPoints.setOrientation(pose.getOrientation());
                }
            }
        }
    }

    private void calculatePlanes(Pose pose) {

        List<Vector3> points = pointCloudManager.get2DPointArrayList();
        supportingPoints = new ArrayList<>();
        Stack<Vector3> hullVertices = new Stack<>();

        for (int i = 0; i < 8; i++) {

            // skip iterating when enough points are matched
            if (points.size() < 10) {
                break;
            }

            // 30% of left points need to be supported
            int sufficientSupport = (int) (points.size() * 0.30);

            // detect plane in hesse normal form
            RANSAC.HessePlane hessePlane = RANSAC.detectPlane(points, 0.03f, 10, sufficientSupport);
            points = RANSAC.notSupportingPoints;
            Log.d(tag, "Found potential Plane :" + hessePlane.toString());

            // skip plane if not sufficient support by points
            if (RANSAC.supportingPoints.size() < sufficientSupport || RANSAC.supportingPoints.size() < 4) {
                continue;
            }

            // translate points to GL coordinates depending con camera position
            Matrix4 transformation = Matrix4.createTranslationMatrix(pose.getPosition()).rotate(pose.getOrientation());

            // find cluster in selected Points
//            KDTree tree = new KDTree(RANSAC.supportingPoints, KDTree.getInitialMedian(RANSAC.supportingPoints));

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
                    Vector3 point = c.getPoints().get(j);
                    supportingPoints.add(point.clone());

                    Vector3 transformedPoint = point.clone();
                    transformedPoint.multiply(transformation);
                    innerHullPoints[j] = new Point3d(transformedPoint.x, transformedPoint.y, transformedPoint.z);
                }

                QuickHull3D hull = new QuickHull3D();
                hull.build(innerHullPoints);
                Point3d[] vertices = hull.getVertices();
                int[][] faces = hull.getFaces();

                for (int[] face : faces) {
                    hullVertices.add(new Vector3(vertices[face[0]].x, vertices[face[0]].y, vertices[face[0]].z));
                    hullVertices.add(new Vector3(vertices[face[1]].x, vertices[face[1]].y, vertices[face[1]].z));
                    hullVertices.add(new Vector3(vertices[face[2]].x, vertices[face[2]].y, vertices[face[2]].z));
                }
            }

        }
        if (hullVertices.size() > 0) {
            newHullVertices = hullVertices;
            shouldUpdatePolygon = true;
        }
    }
}
