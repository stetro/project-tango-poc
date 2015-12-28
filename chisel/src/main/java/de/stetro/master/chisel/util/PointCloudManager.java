package de.stetro.master.chisel.util;


import android.util.Log;

import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class PointCloudManager {
    private static final String tag = PointCloudManager.class.getSimpleName();


    private final TangoCameraIntrinsics tangoCameraIntrinsics;
    private final TangoXyzIjData xyzIjData;
    private TangoPoseData devicePoseAtCloudTime;
    private double lastCloudTime = 0;
    private double newCloudTime = 0;

    public PointCloudManager(TangoCameraIntrinsics intrinsics) {
        tangoCameraIntrinsics = intrinsics;
        xyzIjData = new TangoXyzIjData();
    }

    public TangoCameraIntrinsics getTangoCameraIntrinsics() {
        return tangoCameraIntrinsics;
    }

    public TangoPoseData getDevicePoseAtCloudTime() {
        return devicePoseAtCloudTime;
    }


    public synchronized void updateXyzIjData(TangoXyzIjData from, TangoPoseData xyzIjPose) {
        devicePoseAtCloudTime = xyzIjPose;
        this.newCloudTime = from.timestamp;

        if (xyzIjData.xyz == null || xyzIjData.xyz.capacity() < from.xyzCount * 3) {
            xyzIjData.xyz = ByteBuffer.allocateDirect(from.xyzCount * 3 * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
        } else {
            xyzIjData.xyz.rewind();
        }

        xyzIjData.xyzCount = from.xyzCount;
        xyzIjData.timestamp = from.timestamp;

        from.xyz.rewind();
        xyzIjData.xyz.put(from.xyz);
        xyzIjData.xyz.rewind();
        from.xyz.rewind();
    }

    public synchronized void fillCurrentPoints(Points currentPoints, Pose pose) {
        currentPoints.updatePoints(xyzIjData.xyz, xyzIjData.xyzCount);
        currentPoints.setPosition(pose.getPosition());
        currentPoints.setOrientation(pose.getOrientation().clone());
        lastCloudTime = newCloudTime;
    }

    public synchronized boolean hasNewPoints() {
        return newCloudTime != lastCloudTime;
    }

    public synchronized float[] getPoints() {
        float[] floats = new float[xyzIjData.xyzCount * 3];
        xyzIjData.xyz.position(0);
        for (int i = 0; i < xyzIjData.xyzCount; i++) {
            floats[i * 3] = xyzIjData.xyz.get();
            floats[i * 3 + 1] = xyzIjData.xyz.get();
            floats[i * 3 + 2] = xyzIjData.xyz.get();
        }
        return floats;
    }

    /*

    final Matrix4 transformation = Matrix4.createTranslationMatrix(pose.getPosition()).rotate(pose.getOrientation());
        float[] floats = new float[xyzIjData.xyzCount * 3];
        xyzIjData.xyz.position(0);
        for (int i = 0; i < xyzIjData.xyzCount; i++) {
            Vector3 vector3 = new Vector3(xyzIjData.xyz.get(), xyzIjData.xyz.get(), xyzIjData.xyz.get());
            vector3.multiply(transformation);
            floats[i * 3] = (float) vector3.x;
            floats[i * 3 + 1] = (float) vector3.y;
            floats[i * 3 + 2] = (float) vector3.z;
        }
        return floats;
    }
    */
}