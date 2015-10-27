package de.stetro.master.masterprototype;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class PointCloudManager {
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POINT_TO_XYZ = 3;
    private final Object mPointCloudLock;
    private PointCloudData mCallbackPointCloudData;
    private PointCloudData mSharedPointCloudData;
    private PointCloudData mRenderPointCloudData;
    private boolean mSwapSignal;
    private double currentTimestamp = 0;
    private double newTimestamp = 0;

    public PointCloudManager(int maxDepthPoints) {
        mSwapSignal = false;
        setupBuffers(maxDepthPoints);
        mPointCloudLock = new Object();
    }


    /**
     * Sets up three buffers namely, Callback, Shared and Render buffers allocated with maximum
     * number of points a point cloud can have.
     *
     * @param maxDepthPoints
     */
    private void setupBuffers(int maxDepthPoints) {
        mCallbackPointCloudData = new PointCloudData();
        mSharedPointCloudData = new PointCloudData();
        mRenderPointCloudData = new PointCloudData();
        mCallbackPointCloudData.floatBuffer = ByteBuffer
                .allocateDirect(maxDepthPoints * BYTES_PER_FLOAT * POINT_TO_XYZ)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mSharedPointCloudData.floatBuffer = ByteBuffer
                .allocateDirect(maxDepthPoints * BYTES_PER_FLOAT * POINT_TO_XYZ)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mRenderPointCloudData.floatBuffer = ByteBuffer
                .allocateDirect(maxDepthPoints * BYTES_PER_FLOAT * POINT_TO_XYZ)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    /**
     * Updates the callbackbuffer with latest pointcloud and swaps the
     *
     * @param callbackBuffer
     * @param pointCount
     * @param timestamp
     */
    public void updateCallbackBufferAndSwap(FloatBuffer callbackBuffer, int pointCount, double timestamp) {
        newTimestamp = timestamp;
        mSharedPointCloudData.floatBuffer.position(0);
        mCallbackPointCloudData.floatBuffer.put(callbackBuffer);
        synchronized (mPointCloudLock) {
            FloatBuffer temp = mSharedPointCloudData.floatBuffer;
            mSharedPointCloudData.floatBuffer = mCallbackPointCloudData.floatBuffer;
            mSharedPointCloudData.pointCount = mCallbackPointCloudData.pointCount;
            mCallbackPointCloudData.floatBuffer = temp;
            mCallbackPointCloudData.pointCount = pointCount;
            mSwapSignal = true;
        }
    }

    /**
     * Returns a shallow copy of latest Point Cloud Render buffer.If there is a swap signal available
     * SharedPointCloud buffer is swapped with Render buffer and it is returned.
     *
     * @return PointClouData which contains a reference to latest PointCloud Floatbuffer and count.
     */
    public PointCloudData updateAndGetLatestPointCloudRenderBuffer() {
        synchronized (mPointCloudLock) {
            if (mSwapSignal) {
                FloatBuffer temp = mRenderPointCloudData.floatBuffer;
                int tempCount = mRenderPointCloudData.pointCount;
                mRenderPointCloudData.floatBuffer = mSharedPointCloudData.floatBuffer;
                mRenderPointCloudData.pointCount = mSharedPointCloudData.pointCount;
                mSharedPointCloudData.floatBuffer = temp;
                mSharedPointCloudData.pointCount = tempCount;
                mSwapSignal = false;
            }
        }
        return mRenderPointCloudData;
    }

    public void pointCloudRed() {
        currentTimestamp = newTimestamp;
    }

    public boolean hasNewPoints() {
        return currentTimestamp != newTimestamp;
    }

    /**
     * A class to hold Depth data in a {@link FloatBuffer} and number of points associated with it.
     */
    public class PointCloudData {
        public FloatBuffer floatBuffer;
        public int pointCount;
    }
}