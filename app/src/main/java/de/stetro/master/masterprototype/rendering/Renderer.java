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
import org.rajawali3d.primitives.Line3D;
import org.rajawali3d.primitives.Plane;
import org.rajawali3d.renderer.RajawaliRenderer;

import java.util.Stack;

import de.greenrobot.event.EventBus;
import de.stetro.master.masterprototype.R;
import de.stetro.master.masterprototype.calc.RANSAC;
import de.stetro.master.masterprototype.ui.event.NewPlaneEvent;


public class Renderer extends RajawaliRenderer {
    private static final float POSITION_SCALE_FACTOR = 5.0f;
    private static final String tag = Renderer.class.getSimpleName();
    private final Object pointCloudSynchronise = new Object();
    private final Material white;
    private final Material green;
    private final Material blue;
    private final Material alphaMaterial;
    private final Material red;
    public Vector3 currentPosition = new Vector3();
    private Quaternion currentRotation = new Quaternion();
    private PointCloud pointCloud;
    private Object3D frontObject;
    private Plane plane;
    private boolean refreshPlane = false;
    private DirectionalLight flashLight;
    private Object3D sceneObject;
    private Object3D line;

    public Renderer(Context context) {
        super(context);
        white = Materials.generateWhiteMaterial();
        green = Materials.generateGreenMaterial();
        blue = Materials.generateBlueMaterial();
        red = Materials.generateRedMaterial();
        alphaMaterial = Materials.generateAlphaMaterial();
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

        flashLight = new DirectionalLight(1f, .2f, -1.0f);
        flashLight.setColor(1.0f, 1.0f, 1.0f);
        flashLight.setPower(1);
        getCurrentScene().addLight(flashLight);

        LoaderOBJ loaderOBJ = new LoaderOBJ(this, R.raw.monkey);
        try {
            loaderOBJ.parse();

            sceneObject = loaderOBJ.getParsedObject();
            sceneObject.setMaterial(red);
            sceneObject.setY(5f);
            sceneObject.setScale(1.0);
            sceneObject.setRotation(Vector3.Axis.X, 180);
            getCurrentScene().addChild(sceneObject);
        } catch (ParsingException e) {
            e.printStackTrace();
        }
        frontObject = new Cube(1);
        frontObject.setMaterial(white);
        frontObject.setScale(0.2);
        getCurrentScene().addChild(frontObject);


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

                Stack<Vector3> vector3s = new Stack<>();
                vector3s.add(plane.getPosition());
                vector3s.add(sceneObject.getPosition());
                if (line != null) {
                    getCurrentScene().removeChild(line);
                }
                line = new Line3D(vector3s, 0.1f);
                line.setMaterial(blue);
                getCurrentScene().addChild(line);
            }
        }
    }

    private void extractPlaneFromPointCloudAndTransform(float[][] points) {
        // detect Plane normal and position using Greedy: RANSAC
        float[] planeValues = RANSAC.detectPlane(points, 0.1f, 10, (int) (0.7f * points.length));
        EventBus.getDefault().post(new NewPlaneEvent(RANSAC.supportingPoints.size()));
        // If plane already exists in scene graph - remove
        if (plane != null) {
            getCurrentScene().removeChild(plane);
        }
        plane = new Plane(Math.abs(planeValues[3]) * POSITION_SCALE_FACTOR, Math.abs(planeValues[3]) * POSITION_SCALE_FACTOR, 1, 1);
        plane.setMaterial(alphaMaterial);
        plane.setDoubleSided(true);

        Vector3 normal = new Vector3(planeValues[0], planeValues[1], planeValues[2]);

        // generate in front of camera position for plane
        Vector3 planeCameraDirection = new Vector3(0, 0, 1).multiply(currentRotation.clone().inverse().toRotationMatrix());
        Vector3 planeInFrontOfCamera = currentPosition.clone().add(planeCameraDirection.multiply(-1.2 * POSITION_SCALE_FACTOR * Math.abs(planeValues[3])));
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

        flashLight.setPosition(currentPosition);

        // MONKEY RELATED COMPUTATIONS
        updateFrontObjectTransformations(devicePosition, deviceRotation);

        // POINTCLOUD RELATED COMPUTATIONS
        updatePointCloudTransformations(devicePosition, deviceRotation);


        // HIDE CUBE IF BEHIND PLANE
      /*if (pointCloud != null && plane != null) {

            Vector3 normal = this.plane.getOrientation().multiply(new Vector3(0, 0, 1));

            if (Intersection.isPlaneBetweenPoints(normal, this.plane.getPosition(), sceneObject.getPosition(), currentPosition)) {
                sceneObject.setVisible(false);
            } else {
                sceneObject.setVisible(true);
            }

        }*/
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

    private void updateFrontObjectTransformations(Vector3 devicePosition, Quaternion deviceRotation) {
        // generate in front of camera position for frontObject
        Vector3 monkeyCameraDirection = new Vector3(0.4, 0.2, 1).multiply(deviceRotation.clone().inverse().toRotationMatrix());
        Vector3 monkeyInFrontOfCamera = devicePosition.clone().add(monkeyCameraDirection.multiply(-1.5));

        // generate camera based rotation for frontObject
        Quaternion monkeyObjectOrientation = deviceRotation.clone().inverse();

        frontObject.setPosition(monkeyInFrontOfCamera);
        frontObject.setOrientation(monkeyObjectOrientation);

        // calculate frontObject object related rotation (-90° on x axis of camera orientation)
        Vector3 monkeyCameraHorizontalDirection = new Vector3(1, 0, 0).multiply(deviceRotation.clone().inverse().toRotationMatrix());
        frontObject.rotate(monkeyCameraHorizontalDirection, -45);
    }

}
