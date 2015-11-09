package de.stetro.master.construct.util;


import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.rajawali.Pose;
import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.math.vector.Vector3;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;


public class PointCloudManager {
    private static final String tag = PointCloudManager.class.getSimpleName();


    private final TangoXyzIjData xyzIjData;
    private TangoPoseData devicePoseAtCloudTime;
    private double lastCloudTime = 0;
    private double newCloudTime = 0;

    public PointCloudManager() {
        xyzIjData = new TangoXyzIjData();
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

    public synchronized boolean hasNewPoints() {
        return newCloudTime != lastCloudTime;
    }

    public synchronized ArrayList<Vector3> get2DPointArrayList() {
        ArrayList<Vector3> points = new ArrayList<>();
        xyzIjData.xyz.rewind();
        for (int i = 0; i < xyzIjData.xyzCount; i++) {
            points.add(new Vector3(xyzIjData.xyz.get(), xyzIjData.xyz.get(), xyzIjData.xyz.get()));
        }
        return points;
    }

    public TangoXyzIjData getXyzIjData() {
        return xyzIjData;
    }
}