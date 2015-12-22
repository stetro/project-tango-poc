package de.stetro.master.construct.rendering;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.ar.TangoRajawaliRenderer;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.math.vector.Vector3;

import java.util.Stack;

import de.stetro.master.construct.util.PointCloudManager;

public class PointCloudARRenderer extends TangoRajawaliRenderer {
    private static final int MAX_POINTS = 20000;
    private static final int MAX_COLLECTED_POINTS = 300000;
    private static final String tag = PointCloudARRenderer.class.getSimpleName();
    private Points currentPoints;
    private PointCollection pointCollection;
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

        pointCollection = new PointCollection(MAX_COLLECTED_POINTS);


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (pointCloudManager != null && isRunning) {
                        synchronized (pointCloudManager) {
                            pointCollection.setIsCalculating(true);
                            if (pointCloudManager.hasNewPoints()) {
                                Pose pose = mScenePoseCalcuator.toOpenGLPointCloudPose(pointCloudManager.getDevicePoseAtCloudTime());
                                pointCloudManager.fillCurrentPoints(currentPoints, pose);
                                pointCloudManager.fillCollectedPoints(pointCollection, pose);
                            }
                            pointCollection.setIsCalculating(false);
                        }
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }).start();
    }

    public void capturePoints() {
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

        if (pointCollection.hasNewPolygons() && !pointCollection.isCalculating()) {
            synchronized (pointCloudManager) {
                if (polygon != null) {
                    getCurrentScene().removeChild(polygon);
                }
                Stack<Vector3> faces = new Stack<>();
                pointCollection.getMeshTree().fillPolygons(faces);
                polygon = new Polygon(faces);
                polygon.setTransparent(true);
                polygon.setMaterial(Materials.getTransparentRed());
                polygon.setDoubleSided(true);
                getCurrentScene().addChild(polygon);
            }
        }
    }

    public void setFaces(Stack<Vector3> faces) {

    }

    public void togglePointCloudVisibility() {
        currentPoints.setVisible(!currentPoints.isVisible());
        pointCollection.setVisible(!pointCollection.isVisible());
    }

    public void clearPoints() {
        pointCollection.clear();
        if (polygon != null) {
            getCurrentScene().removeChild(polygon);
        }
    }

    public void toggleAction() {
        isRunning = !isRunning;
        Log.d(tag, "Toggled Reconstruction to " + isRunning);
    }
}
