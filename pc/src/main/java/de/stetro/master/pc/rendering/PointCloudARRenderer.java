package de.stetro.master.pc.rendering;

import android.content.Context;
import android.view.MotionEvent;

import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.ar.TangoRajawaliRenderer;
import com.projecttango.rajawali.renderables.primitives.Points;

import de.stetro.master.pc.ui.MainActivity;
import de.stetro.master.pc.util.PointCloudExporter;
import de.stetro.master.pc.util.PointCloudManager;

public class PointCloudARRenderer extends TangoRajawaliRenderer {
    private static final int MAX_POINTS = 100000;
    private static final int MAX_COLLECTED_POINTS = 300000;
    private Points currentPoints;
    private PointCollection collectedPoints;
    private PointCloudManager pointCloudManager;
    private boolean collectPoints;


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
        currentPoints.setMaterial(Materials.getGreenPointCloudMaterial());

        getCurrentScene().addChild(currentPoints);

        collectedPoints = new PointCollection(MAX_COLLECTED_POINTS);
        collectedPoints.setMaterial(Materials.getBluePointCloudMaterial());

        getCurrentScene().addChild(collectedPoints);
    }

    public void capturePoints() {
        collectPoints = true;
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
            if (collectPoints) {
                collectPoints = false;
                pointCloudManager.fillCollectedPoints(collectedPoints, pose);
            }
            pointCloudManager.fillCurrentPoints(currentPoints, pose);
        }
    }

    public void exportPointCloud(MainActivity mainActivity) {
        PointCloudExporter exporter = new PointCloudExporter(mainActivity, collectedPoints);
        exporter.export();
    }
}
