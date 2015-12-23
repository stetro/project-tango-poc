package de.stetro.master.chisel.rendering;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.ar.TangoRajawaliRenderer;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.math.Matrix;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;

import java.util.Stack;

import de.stetro.master.chisel.JNIInterface;
import de.stetro.master.chisel.util.PointCloudManager;


public class PointCloudARRenderer extends TangoRajawaliRenderer {
    private static final int MAX_POINTS = 20000;
    private static final String tag = PointCloudARRenderer.class.getSimpleName();
    private Points currentPoints;

    private PointCloudManager pointCloudManager;
    private Polygon polygon;
    private boolean isRunning = true;
    private float[] mesh;
    private boolean updateMesh;


    public PointCloudARRenderer(Context context) {
        super(context);
    }

    public void setPointCloudManager(PointCloudManager pointCloudManager) {
        this.pointCloudManager = pointCloudManager;
    }

    @Override
    protected void initScene() {
        super.initScene();
        currentPoints = new Points(MAX_POINTS);
        getCurrentScene().addChild(currentPoints);
    }

    public void capturePoints() {
        if (pointCloudManager != null) {
            long measure = System.currentTimeMillis();
            Pose pose = mScenePoseCalcuator.toOpenGLPointCloudPose(pointCloudManager.getDevicePoseAtCloudTime());
            float[] points = pointCloudManager.getPoints(pose);
            double[] translation = new double[16];
            Matrix.setIdentityM(translation, 0);
            JNIInterface.addPoints(points, poseToTransformation(pose));
            JNIInterface.update();
            mesh = JNIInterface.getMesh();
            updateMesh = true;
            Log.d(tag, "Operation took " + (System.currentTimeMillis() - measure) + "ms");
        }
    }

    private float[] poseToTransformation(Pose pose) {
        float[] result = new float[16];
        Quaternion orientation = pose.getOrientation();
        orientation.conjugate();
        Matrix4 translate = Matrix4.createRotationMatrix(orientation).translate(pose.getPosition());
        translate.toFloatArray(result);
        return result;
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    @Override
    protected synchronized void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);
        if (pointCloudManager != null) {
            if (pointCloudManager.hasNewPoints()) {
                Pose pose = mScenePoseCalcuator.toOpenGLPointCloudPose(pointCloudManager.getDevicePoseAtCloudTime());
                pointCloudManager.fillCurrentPoints(currentPoints, pose);
            }
            if (updateMesh) {
                updateMesh = false;

                synchronized (pointCloudManager) {

                    if (polygon != null) {
                        getCurrentScene().removeChild(polygon);
                    }
                    Stack<Vector3> faces = new Stack<>();
                    for (int i = 0; i < mesh.length / 3; i++) {
                        faces.add(new Vector3(mesh[i * 3], mesh[i * 3 + 1], mesh[i * 3 + 2]));
                    }
                    polygon = new Polygon(faces);
                    polygon.setTransparent(true);
                    polygon.setMaterial(Materials.getTransparentRed());
                    polygon.setDoubleSided(true);
                    getCurrentScene().addChild(polygon);
                }
            }
        }
    }

    public void setFaces(Stack<Vector3> faces) {
        float[] mesh = JNIInterface.getMesh();
        for (int i = 0; i < mesh.length / 3; i++) {
            faces.add(new Vector3(mesh[i * 3], mesh[i * 3 + 1], mesh[i * 3 + 2]));
        }
    }

    public void togglePointCloudVisibility() {
        currentPoints.setVisible(!currentPoints.isVisible());
    }

    public void clearPoints() {
        if (polygon != null) {
            getCurrentScene().removeChild(polygon);
            JNIInterface.clear();
        }
    }

    public void toggleAction() {
        isRunning = !isRunning;
        Log.d(tag, "Toggled Reconstruction to " + isRunning);
    }
}
