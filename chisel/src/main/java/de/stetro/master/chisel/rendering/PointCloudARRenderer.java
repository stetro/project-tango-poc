package de.stetro.master.chisel.rendering;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.google.atap.tangoservice.TangoPoseData;
import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.ScenePoseCalcuator;
import com.projecttango.rajawali.ar.TangoRajawaliRenderer;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.math.Matrix;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Cube;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import de.stetro.master.chisel.JNIInterface;
import de.stetro.master.chisel.util.PLYExporter;
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
    private Cube cube;


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

        cube = new Cube(0.1f);
        cube.setMaterial(Materials.getGreenMaterial());
        getCurrentScene().addChild(cube);
    }

    public void capturePoints() {
        if (pointCloudManager != null) {
            long measure = System.currentTimeMillis();
            Pose pose = mScenePoseCalcuator.toOpenGLPointCloudPose(pointCloudManager.getDevicePoseAtCloudTime());
            float[] points = pointCloudManager.getPoints();
            Matrix4 transformation = poseToTransformation(pose);
            Vector3 aPoint = new Vector3(points[0], points[1], points[2]);
            cube.setPosition(aPoint.multiply(transformation));
            float[] values = transformation.getFloatValues();
            float[] copy = swapMatrixFloatRepresentation(values);
            JNIInterface.addPoints(points, copy);
            JNIInterface.update();
            mesh = JNIInterface.getMesh();
            updateMesh = true;
            Log.d(tag, "Operation took " + (System.currentTimeMillis() - measure) + "ms");
        }
    }

    private float[] swapMatrixFloatRepresentation(float[] values) {
        float[] copy = new float[16];
        copy[0] = values[Matrix4.M00];
        copy[1] = values[Matrix4.M01];
        copy[2] = values[Matrix4.M02];
        copy[3] = values[Matrix4.M03];
        copy[4] = values[Matrix4.M10];
        copy[5] = values[Matrix4.M11];
        copy[6] = values[Matrix4.M12];
        copy[7] = values[Matrix4.M13];
        copy[8] = values[Matrix4.M20];
        copy[9] = values[Matrix4.M21];
        copy[10] = values[Matrix4.M22];
        copy[11] = values[Matrix4.M23];
        copy[12] = values[Matrix4.M30];
        copy[13] = values[Matrix4.M31];
        copy[14] = values[Matrix4.M32];
        copy[15] = values[Matrix4.M33];
        return copy;
    }


    public Matrix4 poseToTransformation(Pose pose) {
        return Matrix4.createRotationMatrix(pose.getOrientation()).translate(pose.getPosition());
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

    public void exportMesh() {
        if (mesh.length > 0) {
            List<Vector3> vertices = new ArrayList<>();
            for (int i = 0; i < mesh.length / 3; i++) {
                vertices.add(new Vector3(mesh[i * 3], mesh[i * 3 + 1], mesh[i * 3 + 2]));
            }
            PLYExporter plyExporter = new PLYExporter(getContext(), vertices);
            plyExporter.export();
        }
    }
}
