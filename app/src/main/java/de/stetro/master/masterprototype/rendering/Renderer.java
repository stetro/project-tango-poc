package de.stetro.master.masterprototype.rendering;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import org.rajawali3d.Object3D;
import org.rajawali3d.cameras.Camera;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.materials.Material;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Cube;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.renderer.RajawaliRenderer;

import de.stetro.master.masterprototype.R;
import de.stetro.master.masterprototype.calc.RANSAC;


public class Renderer extends RajawaliRenderer {
    private static final double POSITION_SCALE_FACTOR = 5.0;
    private static final String tag = Renderer.class.getSimpleName();
    private final Object pointCloudSynchronise = new Object();
    private final Material white;
    private final Material green;
    private final Material red;
    public Vector3 currentPosition = new Vector3();
    private Quaternion currentRotation = new Quaternion();
    private PointCloud pointCloud;
    private Object3D monkey;
    private Plane plane;
    private boolean refreshPlane = false;

    public Renderer(Context context) {
        super(context);
        white = Materials.generateWhiteMaterial();
        green = Materials.generateGreenMaterial();
        red = Materials.generateRedMaterial();
        setFrameRate(60);
    }

    public void setRefreshPlane(boolean refreshPlane) {
        this.refreshPlane = refreshPlane;
    }

    @Override
    protected void initScene() {

        DirectionalLight directionalLight = new DirectionalLight(1f, .2f, -1.0f);
        directionalLight.setColor(1.0f, 1.0f, 1.0f);
        directionalLight.setPower(2);
        getCurrentScene().addLight(directionalLight);

        Cube cube = new Cube(1);
        cube.setMaterial(white);
        cube.setY(5f);
        getCurrentScene().addChild(cube);

        LoaderOBJ loaderOBJ = new LoaderOBJ(this, R.raw.monkey);
        try {
            loaderOBJ.parse();
            monkey = loaderOBJ.getParsedObject();
            monkey.setMaterial(white);
            monkey.setScale(0.2);
            getCurrentScene().addChild(monkey);
        } catch (ParsingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
    }

    @Override
    public void onTouchEvent(MotionEvent event) {
    }

    @Override
    public void onRender(final long elapsedTime, final double deltaTime) {
        synchronized (pointCloudSynchronise) {
            super.onRender(elapsedTime, deltaTime);
        }
    }

    public void setPointCloud(TangoXyzIjData xyzIj) {
        Log.d(tag, "loaded " + (xyzIj.xyzCount) + " points");
        float[][] points = new float[xyzIj.xyzCount][3];
        synchronized (pointCloudSynchronise) {
            if (pointCloud != null) {
                getCurrentScene().removeChild(pointCloud);
            }
            pointCloud = new PointCloud();
            pointCloud.setMaterial(green);
            for (int i = 0; xyzIj.xyz.hasRemaining(); i++) {
                float x = xyzIj.xyz.get();
                float y = xyzIj.xyz.get();
                float z = xyzIj.xyz.get();
                points[i][0] = x;
                points[i][1] = y;
                points[i][2] = z;
                pointCloud.add(new Point(x, y, z));
            }
            pointCloud.init();
            getCurrentScene().addChild(pointCloud);

            if (refreshPlane) {
                refreshPlane = false;
                extractPlaneFromPointCloudAndTransform(points);
            }
        }
    }

    private void extractPlaneFromPointCloudAndTransform(float[][] points) {
        // detect Plane with Greedy RANSAC
        float[] planeValues = RANSAC.detectPlane(points, 0.1f, 10, (int) (0.7f * points.length));
        if (plane != null) {
            getCurrentScene().removeChild(plane);
        }
        plane = new Plane(1, 1, 1, 1);
        plane.setMaterial(red);
        plane.setDoubleSided(true);

        Vector3 normal = new Vector3(planeValues[0], planeValues[1], planeValues[2]);

        // generate in front of camera position for plane
        Vector3 planeCameraDirection = new Vector3(0, 0, 1).multiply(currentRotation.clone().inverse().toRotationMatrix());
        Vector3 planeInFrontOfCamera = currentPosition.clone().add(planeCameraDirection.multiply(-1.3 * POSITION_SCALE_FACTOR * Math.abs(planeValues[3])));
        plane.setPosition(planeInFrontOfCamera);

        // calculate plane object related rotation between Plane and normal
        normal.rotateBy(pointCloud.getOrientation());
        plane.setRotation(Quaternion.createFromRotationBetween(normal, new Vector3(0, 0, 1)));

        getCurrentScene().addChild(plane);
    }

    public void updateCamera(TangoPoseData pose) {
        Camera camera = getCurrentCamera();

        // get current device position and update camera
        Vector3 devicePosition = new Vector3(pose.translation);
        devicePosition.multiply(POSITION_SCALE_FACTOR);
        camera.setPosition(devicePosition);
        currentPosition = devicePosition.clone();

        // get current device rotation as quaternion and update camera with INVERSE!
        Quaternion deviceRotation = new Quaternion(
                pose.rotation[TangoPoseData.INDEX_ROTATION_W],
                pose.rotation[TangoPoseData.INDEX_ROTATION_X],
                pose.rotation[TangoPoseData.INDEX_ROTATION_Y],
                pose.rotation[TangoPoseData.INDEX_ROTATION_Z]);
        camera.setRotation(deviceRotation.clone().inverse());
        currentRotation = deviceRotation.clone();

        // MONKEY RELATED COMPUTATIONS
        updateMonkeyTransformations(devicePosition, deviceRotation);

        // POINTCLOUD RELATED COMPUTATIONS
        updatePointCloudTransformations(devicePosition, deviceRotation);
    }

    private void updatePointCloudTransformations(Vector3 devicePosition, Quaternion deviceRotation) {
        if (pointCloud != null) {
            // generate camera based rotation for pointCloud
            Quaternion pointCloudObjectOrientation = deviceRotation.clone().inverse();

            // generate in front of camera position for pointCloud
            Vector3 pointCloudCameraDirection = new Vector3(0, 0, 1).multiply(deviceRotation.clone().inverse().toRotationMatrix());
            Vector3 pointCloudInFrontOfCamera = devicePosition.clone().add(pointCloudCameraDirection.multiply(0.4));

            pointCloud.setPosition(pointCloudInFrontOfCamera);
            pointCloud.setOrientation(pointCloudObjectOrientation);

            // calculate point cloud object related rotation (180° on Y axis of camera orientation)
            Vector3 pointCloudCameraHorizontalDirection = new Vector3(1, 0, 0).multiply(deviceRotation.clone().inverse().toRotationMatrix());
            pointCloud.rotate(pointCloudCameraHorizontalDirection, 180);
        }
    }

    private void updateMonkeyTransformations(Vector3 devicePosition, Quaternion deviceRotation) {
        // generate in front of camera position for monkey
        Vector3 monkeyCameraDirection = new Vector3(0.4, 0.2, 1).multiply(deviceRotation.clone().inverse().toRotationMatrix());
        Vector3 monkeyInFrontOfCamera = devicePosition.clone().add(monkeyCameraDirection.multiply(-1.5));

        // generate camera based rotation for monkey
        Quaternion monkeyObjectOrientation = deviceRotation.clone().inverse();

        monkey.setPosition(monkeyInFrontOfCamera);
        monkey.setOrientation(monkeyObjectOrientation);

        // calculate monkey object related rotation (-90° on x axis of camera orientation)
        Vector3 monkeyCameraHorizontalDirection = new Vector3(1, 0, 0).multiply(deviceRotation.clone().inverse().toRotationMatrix());
        monkey.rotate(monkeyCameraHorizontalDirection, -90);
    }

}
