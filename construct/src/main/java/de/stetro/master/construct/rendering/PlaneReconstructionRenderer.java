package de.stetro.master.construct.rendering;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;

import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;
import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.ar.TangoRajawaliRenderer;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Stack;

import de.stetro.master.construct.calc.RANSAC;
import de.stetro.master.construct.util.PointCloudManager;

public class PlaneReconstructionRenderer extends TangoRajawaliRenderer {
    private static final int MAX_POINTS = 100000;
    private static final String tag = PlaneReconstructionRenderer.class.getSimpleName();
    private Points currentPoints;
    private PointCloudManager pointCloudManager;
    private boolean willCalculatePlanes;
    private Points planePoints;
    private Polygon polygon;

    public PlaneReconstructionRenderer(Context context, PointCloudManager pointCloudManager) {
        super(context);
        this.pointCloudManager = pointCloudManager;
    }


    @Override
    protected void initScene() {
        super.initScene();

        currentPoints = new Points(MAX_POINTS);
        currentPoints.setMaterial(Materials.getGreenPointCloudMaterial());
        getCurrentScene().addChild(currentPoints);

        planePoints = new Points(MAX_POINTS);
        planePoints.setMaterial(Materials.getBluePointCloudMaterial());
        getCurrentScene().addChild(planePoints);
    }

    public void capturePoints() {
        willCalculatePlanes = true;
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
            if (willCalculatePlanes) {
                willCalculatePlanes = false;
                calculatePlanes(pose);
            }
//            pointCloudManager.fillCurrentPoints(currentPoints, pose);
        }
    }

    private void calculatePlanes(Pose pose) {
        float[][] points = pointCloudManager.get2DPointArray();
        int sufficientSupport = (int) (pointCloudManager.getXyzIjData().xyzCount * 0.3);
        float[] plane = RANSAC.detectPlane(points, 0.02f, 5, sufficientSupport);
        Log.d(tag, "1st Iteration :" + Arrays.toString(plane)); // {n0,n1,n2,d}

        Point3d[] supportingPoints = new Point3d[RANSAC.supportingPoints.size()];
        Matrix4 transformation = Matrix4.createTranslationMatrix(pose.getPosition()).rotate(pose.getOrientation());
        for (int i = 0; i < RANSAC.supportingPoints.size(); i++) {
            float[] point = RANSAC.supportingPoints.get(i);
            Vector3 v = new Vector3(point[0], point[1], point[2]);
            v.multiply(transformation);
            supportingPoints[i] = new Point3d(v.x, v.y, v.z);
        }
        QuickHull3D hull = new QuickHull3D();
        hull.build(supportingPoints);
        Point3d[] vertices = hull.getVertices();
        int[][] faces = hull.getFaces();

        if (polygon != null) {
            getCurrentScene().removeChild(polygon);
        }

        Stack<Vector3> hullVertices = new Stack<>();
        for (int[] face : faces) {
            hullVertices.add(new Vector3(vertices[face[0]].x, vertices[face[0]].y, vertices[face[0]].z));
            hullVertices.add(new Vector3(vertices[face[1]].x, vertices[face[1]].y, vertices[face[1]].z));
            hullVertices.add(new Vector3(vertices[face[2]].x, vertices[face[2]].y, vertices[face[2]].z));
        }
        polygon = new Polygon(hullVertices);
        polygon.setMaterial(Materials.getRedMaterial());
        getCurrentScene().addChild(polygon);

        FloatBuffer f = FloatBuffer.allocate(RANSAC.supportingPoints.size() * 3);
        for (int i = 0; i < RANSAC.supportingPoints.size(); i++) {
            f.put(RANSAC.supportingPoints.get(i)[0]);
            f.put(RANSAC.supportingPoints.get(i)[1]);
            f.put(RANSAC.supportingPoints.get(i)[2]);
        }
        planePoints.updatePoints(f, RANSAC.supportingPoints.size());
        planePoints.setPosition(pose.getPosition());
        planePoints.setOrientation(pose.getOrientation());

        f = FloatBuffer.allocate(RANSAC.notSupportingPoints.size() * 3);
        for (int i = 0; i < RANSAC.notSupportingPoints.size(); i++) {
            f.put(RANSAC.notSupportingPoints.get(i)[0]);
            f.put(RANSAC.notSupportingPoints.get(i)[1]);
            f.put(RANSAC.notSupportingPoints.get(i)[2]);
        }
        currentPoints.updatePoints(f, RANSAC.notSupportingPoints.size());
        currentPoints.setPosition(pose.getPosition());
        currentPoints.setOrientation(pose.getOrientation());
    }


}
