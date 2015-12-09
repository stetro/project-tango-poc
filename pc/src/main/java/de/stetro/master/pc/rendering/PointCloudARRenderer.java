package de.stetro.master.pc.rendering;

import android.content.Context;
import android.view.MotionEvent;

import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.ar.TangoRajawaliRenderer;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.math.vector.Vector3;

import java.util.Stack;

import de.stetro.master.pc.ui.MainActivity;
import de.stetro.master.pc.util.PointCloudExporter;
import de.stetro.master.pc.util.PointCloudManager;
import de.stetro.master.pc.util.ReconstructionBuilder;

public class PointCloudARRenderer extends TangoRajawaliRenderer {
    private static final int MAX_POINTS = 100000;
    private static final int MAX_COLLECTED_POINTS = 300000;
    private static final String tag = PointCloudARRenderer.class.getSimpleName();
    private Points currentPoints;
    private PointCollection pointCollection;
    private PointCloudManager pointCloudManager;
    private boolean collectPoints;
    private Stack<Vector3> faces;
    private boolean updateFaces;
    private Polygon polygon;
    private MeshTree meshTree;


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

        pointCollection = new PointCollection(MAX_COLLECTED_POINTS);
        pointCollection.setMaterial(Materials.getBluePointCloudMaterial());
        getCurrentScene().addChild(pointCollection);

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
            if (true) {
                collectPoints = true;
                pointCloudManager.fillCollectedPoints(pointCollection, pose);
            }
        }
        if (true) {
//            updateFaces = false;
            if (polygon != null) {
                getCurrentScene().removeChild(polygon);
            }
            Stack<Vector3> faces = new Stack<>();
            pointCollection.getMeshTree().fillPolygons(faces);
            polygon = new Polygon(faces);
            polygon.setTransparent(true);
            polygon.setMaterial(Materials.getTransparentRed());
            getCurrentScene().addChild(polygon);
        }
    }

    public void exportPointCloud(MainActivity mainActivity) {
        PointCloudExporter exporter = new PointCloudExporter(mainActivity, pointCollection);
        exporter.export();
    }

    public void reconstruct(MainActivity mainActivity) {
        ReconstructionBuilder builder = new ReconstructionBuilder(mainActivity, pointCollection, this);
        builder.reconstruct();
    }

    public void setFaces(Stack<Vector3> faces) {
        this.faces = faces;
        updateFaces = true;
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
}
