package de.stetro.master.constructnative.util;


import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.renderables.primitives.Points;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.stetro.master.constructnative.rendering.PointCollection;


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
        currentPoints.setOrientation(pose.getOrientation());
        lastCloudTime = newCloudTime;
    }

    public synchronized void fillCollectedPoints(PointCollection collectedPoints, Pose pose) {
        collectedPoints.updatePoints(xyzIjData.xyz, xyzIjData.xyzCount, pose);
    }

    public synchronized boolean hasNewPoints() {
        return newCloudTime != lastCloudTime;
    }

}