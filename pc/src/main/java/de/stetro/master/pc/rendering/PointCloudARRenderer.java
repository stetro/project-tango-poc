package de.stetro.master.pc.rendering;

import android.content.Context;
import android.view.MotionEvent;

import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.ar.TangoRajawaliRenderer;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.math.vector.Vector3;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Stack;

import de.stetro.master.pc.calc.OctTree;
import de.stetro.master.pc.marchingcubes.Cube;
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

        OctTree ot = new OctTree(new Vector3(-2.0, -2.0, -2.0), 4.0, 8);
        ot.put(new Vector3(0.0, 0.0, 0.0));
        List<Cube> cubes = ot.getCubes(0);
        Stack<Vector3> faces = new Stack<>();
        for (Cube cube : cubes) {
            cube.getFaces(faces);
        }
        Points points = new Points(1000);
        FloatBuffer f = FloatBuffer.allocate(1000 * 3);
        for (Vector3 face : faces) {
            f.put((float) face.x);
            f.put((float) face.y);
            f.put((float) face.z);
        }
        points.updatePoints(f, faces.size());
        points.setMaterial(Materials.getBlueMaterial());
        Polygon p = new Polygon(faces);
        p.setTransparent(true);
        p.setMaterial(Materials.getRedMaterial());
        getCurrentScene().addChild(points);
        getCurrentScene().addChild(p);
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
                pointCloudManager.fillCollectedPoints(pointCollection, pose);
            }
            pointCloudManager.fillCurrentPoints(currentPoints, pose);
        }
        if (updateFaces) {
            updateFaces = false;
            if (polygon != null) {
                getCurrentScene().removeChild(polygon);
            }
            polygon = new Polygon(faces);
            polygon.setMaterial(Materials.getTransparentClippingMaterial());
            polygon.setTransparent(true);
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
