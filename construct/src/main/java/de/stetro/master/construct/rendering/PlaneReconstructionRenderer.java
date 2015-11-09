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

        /**
         * 1ST POTENTIAL ITERATION
         */

        ArrayList<Vector3> points = pointCloudManager.get2DPointArrayList();
        int sufficientSupport = (int) (pointCloudManager.getXyzIjData().xyzCount * 0.35);
        RANSAC.HessePlane plane = RANSAC.detectPlane(points, 0.03f, 10, sufficientSupport);
        Log.d(tag, "1st Iteration :" + plane.toString()); // {n0,n1,n2,d}
        Point3d[] supportingPoints = new Point3d[RANSAC.supportingPoints.size()];
        if (supportingPoints.length < sufficientSupport|| supportingPoints.length < 3) {
            if (polygon2 != null) {
                polygon2.setVisible(false);
            }
            return;
        }
        Matrix4 transformation = Matrix4.createTranslationMatrix(pose.getPosition()).rotate(pose.getOrientation());
        for (int i = 0; i < RANSAC.supportingPoints.size(); i++) {
            Vector3 v = RANSAC.supportingPoints.get(i).clone();
            v.multiply(transformation);
            supportingPoints[i] = new Point3d(v.x, v.y, v.z);
        }
        QuickHull3D hull = new QuickHull3D();
        hull.build(supportingPoints);
        Point3d[] vertices = hull.getVertices();
        int[][] faces = hull.getFaces();

        if (polygon != null) {
            getCurrentScene().removeChild(polygon);
        }

        Stack<Vector3> hullVertices = new Stack<>();
        for (int[] face : faces) {
            hullVertices.add(new Vector3(vertices[face[0]].x, vertices[face[0]].y, vertices[face[0]].z));
            hullVertices.add(new Vector3(vertices[face[1]].x, vertices[face[1]].y, vertices[face[1]].z));
            hullVertices.add(new Vector3(vertices[face[2]].x, vertices[face[2]].y, vertices[face[2]].z));
        }
        polygon = new Polygon(hullVertices);
        polygon.setMaterial(Materials.getRedMaterial());
        polygon.setTransparent(true);
        getCurrentScene().addChild(polygon);

        FloatBuffer f = FloatBuffer.allocate(RANSAC.supportingPoints.size() * 3);
        for (int i = 0; i < RANSAC.supportingPoints.size(); i++) {
            f.put((float) RANSAC.supportingPoints.get(i).x);
            f.put((float) RANSAC.supportingPoints.get(i).y);
            f.put((float) RANSAC.supportingPoints.get(i).z);
        }
        polygonPoints.updatePoints(f, RANSAC.supportingPoints.size());
        polygonPoints.setPosition(pose.getPosition());
        polygonPoints.setOrientation(pose.getOrientation());


        /**
         * 2ND POTENTIAL ITERATION
         */

        sufficientSupport = (int) (RANSAC.notSupportingPoints.size() * 0.3);
        plane = RANSAC.detectPlane(RANSAC.notSupportingPoints, 0.03f, 10, sufficientSupport);
        Log.d(tag, "2nd Iteration :" + plane.toString()); // {n0,n1,n2,d}
        supportingPoints = new Point3d[RANSAC.supportingPoints.size()];
        if (supportingPoints.length < sufficientSupport|| supportingPoints.length < 3) {
            if (polygon2 != null) {
                polygon2.setVisible(false);
            }
            return;
        }
        transformation = Matrix4.createTranslationMatrix(pose.getPosition()).rotate(pose.getOrientation());
        for (int i = 0; i < RANSAC.supportingPoints.size(); i++) {
            Vector3 v = RANSAC.supportingPoints.get(i).clone();
            v.multiply(transformation);
            supportingPoints[i] = new Point3d(v.x, v.y, v.z);
        }
        hull = new QuickHull3D();
        hull.build(supportingPoints);
        vertices = hull.getVertices();
        faces = hull.getFaces();

        if (polygon2 != null) {
            getCurrentScene().removeChild(polygon2);
        }

        hullVertices = new Stack<>();
        for (int[] face : faces) {
            hullVertices.add(new Vector3(vertices[face[0]].x, vertices[face[0]].y, vertices[face[0]].z));
            hullVertices.add(new Vector3(vertices[face[1]].x, vertices[face[1]].y, vertices[face[1]].z));
            hullVertices.add(new Vector3(vertices[face[2]].x, vertices[face[2]].y, vertices[face[2]].z));
        }
        polygon2 = new Polygon(hullVertices);
        polygon2.setMaterial(Materials.getBlueMaterial());
        polygon2.setTransparent(true);
        getCurrentScene().addChild(polygon2);

        f = FloatBuffer.allocate(RANSAC.supportingPoints.size() * 3);
        for (int i = 0; i < RANSAC.supportingPoints.size(); i++) {
            Vector3 vector3 = RANSAC.supportingPoints.get(i);
            f.put((float) vector3.x);
            f.put((float) vector3.y);
            f.put((float) vector3.z);
        }
        polygon2Points.updatePoints(f, RANSAC.supportingPoints.size());
        polygon2Points.setPosition(pose.getPosition());
        polygon2Points.setOrientation(pose.getOrientation());


        /**
         * 3RD POTENTIAL ITERATION
         */
        sufficientSupport = (int) (RANSAC.notSupportingPoints.size() * 0.3);
        plane = RANSAC.detectPlane(RANSAC.notSupportingPoints, 0.03f, 10, sufficientSupport);
        Log.d(tag, "3rd Iteration :" + plane.toString()); // {n0,n1,n2,d}
        supportingPoints = new Point3d[RANSAC.supportingPoints.size()];
        if (supportingPoints.length < sufficientSupport || supportingPoints.length < 3) {
            if (polygon3 != null) {
                polygon3.setVisible(false);
            }
            return;
        }

        transformation = Matrix4.createTranslationMatrix(pose.getPosition()).rotate(pose.getOrientation());
        for (int i = 0; i < RANSAC.supportingPoints.size(); i++) {
            Vector3 v = RANSAC.supportingPoints.get(i).clone();
            v.multiply(transformation);
            supportingPoints[i] = new Point3d(v.x, v.y, v.z);
        }
        hull = new QuickHull3D();
        hull.build(supportingPoints);
        vertices = hull.getVertices();
        faces = hull.getFaces();

        if (polygon3 != null) {
            getCurrentScene().removeChild(polygon3);
        }

        hullVertices = new Stack<>();
        for (int[] face : faces) {
            hullVertices.add(new Vector3(vertices[face[0]].x, vertices[face[0]].y, vertices[face[0]].z));
            hullVertices.add(new Vector3(vertices[face[1]].x, vertices[face[1]].y, vertices[face[1]].z));
            hullVertices.add(new Vector3(vertices[face[2]].x, vertices[face[2]].y, vertices[face[2]].z));
        }
        polygon3 = new Polygon(hullVertices);
        polygon3.setMaterial(Materials.getYellowMaterial());
        polygon3.setTransparent(true);
        getCurrentScene().addChild(polygon3);

        f = FloatBuffer.allocate(RANSAC.supportingPoints.size() * 3);
        for (int i = 0; i < RANSAC.supportingPoints.size(); i++) {
            Vector3 vector3 = RANSAC.supportingPoints.get(i);
            f.put((float) vector3.x);
            f.put((float) vector3.y);
            f.put((float) vector3.z);
        }
        polygon3Points.updatePoints(f, RANSAC.supportingPoints.size());
        polygon3Points.setPosition(pose.getPosition());
        polygon3Points.setOrientation(pose.getOrientation());


        f = FloatBuffer.allocate(RANSAC.notSupportingPoints.size() * 3);
        for (int i = 0; i < RANSAC.notSupportingPoints.size(); i++) {
            Vector3 vector3 = RANSAC.notSupportingPoints.get(i);
            f.put((float) vector3.x);
            f.put((float) vector3.y);
            f.put((float) vector3.z);
        }
        currentPoints.updatePoints(f, RANSAC.notSupportingPoints.size());
        currentPoints.setPosition(pose.getPosition());
        currentPoints.setOrientation(pose.getOrientation());

    }


}
