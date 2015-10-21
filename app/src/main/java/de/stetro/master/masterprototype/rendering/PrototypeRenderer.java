package de.stetro.master.masterprototype.rendering;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.projecttango.rajawali.ar.TangoRajawaliRenderer;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.plugins.DepthMaterialPlugin;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Cube;
import org.rajawali3d.primitives.Plane;

import java.nio.FloatBuffer;
import java.util.Arrays;

import de.stetro.master.masterprototype.PointCloudManager;
import de.stetro.master.masterprototype.calc.RANSAC;

public class PrototypeRenderer extends TangoRajawaliRenderer {
    private static final float CAMERA_NEAR = 0.01f;
    private static final float CAMERA_FAR = 200f;
    private static final int MAX_NUMBER_OF_POINTS = 60000;
    private static final String tag = PrototypeRenderer.class.getSimpleName();
    private Plane plane;
    private Points points;
    private PointCloudManager pointCloudManager;
    private Material red;

    public PrototypeRenderer(Context context, PointCloudManager pointCloudManager) {
        super(context);
        this.pointCloudManager = pointCloudManager;
    }

    @Override
    protected void initScene() {
        super.initScene();

        DirectionalLight light = new DirectionalLight(1, 0.2, -1);
        light.setColor(1, 1, 1);
        light.setPower(0.8f);
        light.setPosition(3, 2, 4);
        getCurrentScene().addLight(light);

        red = new Material();
        red.setColor(0xff990000);
        red.enableLighting(true);
        red.setDiffuseMethod(new DiffuseMethod.Lambert());

        Object3D cube = new Cube(0.1f);
        cube.setMaterial(red);
        cube.setPosition(.05f, 0, -0.5f);
        getCurrentScene().addChild(cube);

        plane = new Plane(10, 10, 1, 1);
        plane.setDoubleSided(true);
        plane.setMaterial(red);
        plane.setVisible(false);
        getCurrentScene().addChild(plane);

        Material alphaRed = new Material();
        alphaRed.addPlugin(new DepthMaterialPlugin());
        alphaRed.setColor(new float[]{0.0f, 0.0f, 0.0f, 0.15f});

        points = new Points(MAX_NUMBER_OF_POINTS);
        points.setMaterial(red);
        getCurrentScene().addChild(points);
        getCurrentCamera().setNearPlane(CAMERA_NEAR);
        getCurrentCamera().setFarPlane(CAMERA_FAR);

    }

    @Override
    protected synchronized void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);
        PointCloudManager.PointCloudData renderPointCloudData = pointCloudManager.updateAndGetLatestPointCloudRenderBuffer();
        points.updatePoints(renderPointCloudData.floatBuffer, renderPointCloudData.pointCount);
        Quaternion cameraOrientation = getCurrentCamera().getOrientation().clone();
        Vector3 pointCloudCameraHorizontalDirection = new Vector3(1, 0, 0).multiply(cameraOrientation.toRotationMatrix());
        points.setPosition(getCurrentCamera().getPosition());
        points.setOrientation(cameraOrientation);
        points.rotate(pointCloudCameraHorizontalDirection, 180);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public synchronized void onTouchEvent(MotionEvent event) {
        FloatBuffer pointCloud = pointCloudManager.getCurrentPointCloud();
        int pointCloudCount = pointCloudManager.getCurrentPointCloudCount();
        if (pointCloud != null && pointCloudCount > 100) {
            float[][] points = new float[pointCloudCount][3];
            for (int i = 0; i < pointCloudCount; i++) {
                points[i][0] = pointCloud.get();
                points[i][1] = pointCloud.get();
                points[i][2] = pointCloud.get();
            }
            float[] planeValues = RANSAC.detectPlane(points, 1.0f, 10, (int) (0.7 * points.length));
            Log.d(tag, "plane found with settings " + Arrays.toString(planeValues));

            // place plane
            if (plane != null) {
                getCurrentScene().removeChild(plane);
            }

            Vector3 normal = new Vector3(planeValues[0], planeValues[1], planeValues[2]);
            Vector3 planePosition = normal.multiply(1.0).add(getCurrentCamera().getPosition());
            plane.setPosition(planePosition);
            plane.setVisible(true);

        } else {
            Log.d(tag, "No points captured");
        }
    }
}
