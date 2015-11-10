package de.stetro.master.construct.rendering;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;
import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.ar.TangoRajawaliRenderer;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.materials.Material;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.stetro.master.construct.calc.RANSAC;
import de.stetro.master.construct.util.PointCloudManager;

public class PlaneReconstructionRenderer extends TangoRajawaliRenderer {
    private static final int MAX_POINTS = 100000;
    private static final String tag = PlaneReconstructionRenderer.class.getSimpleName();
    private final PointCloudManager pointCloudManager;
    private Points currentPoints;
    private boolean willCalculatePlanes;
    private Points polygonPoints;
    private Points polygon2Points;
    private Points polygon3Points;
    private Polygon polygon;
    private Polygon polygon2;
    private Polygon polygon3;

    public PlaneReconstructionRenderer(Context context, PointCloudManager pointCloudManager) {
        super(context);
        this.pointCloudManager = pointCloudManager;
    }


    @Override
    protected void initScene() {
        super.initScene();

        currentPoints = new Points(MAX_POINTS);
        currentPoints.setMaterial(Materials.getGreenPointCloudMaterial());
        getCurrentScene().addChild(currentPoints);

        polygonPoints = new Points(MAX_POINTS);
        polygonPoints.setMaterial(Materials.getRedPointCloudMaterial());
        getCurrentScene().addChild(polygonPoints);

        polygon2Points = new Points(MAX_POINTS);
        polygon2Points.setMaterial(Materials.getBluePointCloudMaterial());
        getCurrentScene().addChild(polygon2Points);

        polygon3Points = new Points(MAX_POINTS);
        polygon3Points.setMaterial(Materials.getYellowPointCloudMaterial());
        getCurrentScene().addChild(polygon3Points);
    }

    public void capturePoints() {
        willCalculatePlanes = true;
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
            Pose pose = mScenePoseCalcuator.toOpenGLPointCloudPose(pointCloudManager.getDevicePoseAtCloudTime());
            synchronized (pointCloudManager) {
                if (willCalculatePlanes) {
                    willCalculatePlanes = false;
                    calculatePlanes(pose);
                } else {
//                    currentPoints.updatePoints(pointCloudManager.getXyzIjData().xyz, pointCloudManager.getXyzIjData().xyzCount);
//                    currentPoints.setPosition(pose.getPosition());
//                    currentPoints.setOrientation(pose.getOrientation());
                }
            }
        }
    }

    private void calculatePlanes(Pose pose) {

        List<Vector3> points = pointCloudManager.get2DPointArrayList();
        List<Vector3> supportingPoints = new ArrayList<>();
        Stack<Vector3> hullVertices = new Stack<>();

        for (int i = 0; i < 5; i++) {

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

            Point3d[] innerHullPoints = new Point3d[RANSAC.supportingPoints.size()];
            if (RANSAC.supportingPoints.size() < sufficientSupport || RANSAC.supportingPoints.size() < 3) {
                continue;
            }

            // translate points to GL coordinates depending con camera position
            Matrix4 transformation = Matrix4.createTranslationMatrix(pose.getPosition()).rotate(pose.getOrientation());

            for (int j = 0; j < RANSAC.supportingPoints.size(); j++) {
                supportingPoints.add(RANSAC.supportingPoints.get(j).clone());

                Vector3 v = RANSAC.supportingPoints.get(j).clone();
                v.multiply(transformation);
                innerHullPoints[j] = new Point3d(v.x, v.y, v.z);
            }

            // calculate convex hull and vertices for supporting points
            if (innerHullPoints.length < 4) {
                continue;
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

            if (polygon != null) {
                getCurrentScene().removeChild(polygon);
            }
        }

        if (polygon != null) {
            getCurrentScene().removeChild(polygon);
        }

        if (hullVertices.size() > 0) {
            polygon = new Polygon(hullVertices);
            polygon.setMaterial(Materials.getRedMaterial());
            polygon.setTransparent(true);
            getCurrentScene().addChild(polygon);

            FloatBuffer f = FloatBuffer.allocate(supportingPoints.size() * 3);
            for (int i = 0; i < supportingPoints.size(); i++) {
                f.put((float) supportingPoints.get(i).x);
                f.put((float) supportingPoints.get(i).y);
                f.put((float) supportingPoints.get(i).z);
            }
            polygonPoints.updatePoints(f, supportingPoints.size());
            polygonPoints.setPosition(pose.getPosition());
            polygonPoints.setOrientation(pose.getOrientation());
        }

    }


}
