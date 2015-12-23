package de.stetro.master.chisel.rendering;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.ar.TangoRajawaliRenderer;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.math.vector.Vector3;

import java.util.Stack;

import de.stetro.master.chisel.JNIInterface;
import de.stetro.master.chisel.util.PointCloudManager;


public class PointCloudARRenderer extends TangoRajawaliRenderer {
    private static final int MAX_POINTS = 20000;
    private static final int MAX_COLLECTED_POINTS = 300000;
    private static final String tag = PointCloudARRenderer.class.getSimpleName();
    private Points currentPoints;

    private PointCloudManager pointCloudManager;
    private Polygon polygon;
    private boolean isRunning = true;


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
        JNIInterface.addPoints(new float[0]);
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
        if (pointCloudManager != null && pointCloudManager.hasNewPoints()) {
            Pose pose = mScenePoseCalcuator.toOpenGLCameraPose(pointCloudManager.getDevicePoseAtCloudTime());
            pointCloudManager.fillCurrentPoints(currentPoints, pose);
        }

        synchronized (pointCloudManager) {
            if (polygon != null) {
                getCurrentScene().removeChild(polygon);
            }
            Stack<Vector3> faces = new Stack<>();
            setFaces(faces);
            polygon = new Polygon(faces);
            polygon.setTransparent(true);
            polygon.setMaterial(Materials.getTransparentRed());
            polygon.setDoubleSided(true);
            getCurrentScene().addChild(polygon);
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
