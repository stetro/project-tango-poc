package de.stetro.master.pc.util;


import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class PointCloudManager {
    private static final String tag = PointCloudManager.class.getSimpleName();

    private final TangoCameraIntrinsics tangoCameraIntrinsics;
    private final TangoXyzIjData xyzIjData;
    private TangoPoseData devicePoseAtCloudTime;

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

}