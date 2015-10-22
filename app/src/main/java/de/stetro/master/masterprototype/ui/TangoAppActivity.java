package de.stetro.master.masterprototype.ui;


import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.rajawali.ar.TangoRajawaliView;

import org.rajawali3d.util.RajLog;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import de.stetro.master.masterprototype.PointCloudManager;
import de.stetro.master.masterprototype.rendering.Cubes;
import de.stetro.master.masterprototype.rendering.PrototypeRenderer;
import de.stetro.master.masterprototype.rendering.event.SceneUpdateEvent;

public abstract class TangoAppActivity extends BaseActivity implements View.OnTouchListener {
    protected TangoRajawaliView glView;
    protected Tango tango;
    protected boolean isPermissionGranted;
    protected boolean isConnected;
    protected TangoConfig config;
    protected PointCloudManager pointCloudManager;
    protected PrototypeRenderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RajLog.setDebugEnabled(true);

        glView = new TangoRajawaliView(this);
        tango = new Tango(this);
        config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);

        int maxDepthPoints = config.getInt("max_point_cloud_elements");
        pointCloudManager = new PointCloudManager(maxDepthPoints);
        renderer = new PrototypeRenderer(this, pointCloudManager);
        glView.setEGLContextClientVersion(2);
        glView.setSurfaceRenderer(renderer);
        glView.setOnTouchListener(this);
    }

    protected void startAugmentedreality() {
        if (!isConnected) {
            glView.connectToTangoCamera(tango, TangoCameraIntrinsics.TANGO_CAMERA_COLOR);

            tango.connect(config);
            isConnected = true;
            ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<>();
            tango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {
                @Override
                public void onPoseAvailable(TangoPoseData pose) {

                }

                @Override
                public void onFrameAvailable(int cameraId) {
                    if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                        glView.onFrameAvailable();
                    }
                }

                @Override
                public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                    synchronized (renderer) {
                        SceneUpdateEvent e = new SceneUpdateEvent();
                        e.setPointCloundPointsCount(xyzIj.xyzCount);
                        e.setCubeCount(renderer.getCubes().getCubeCount());
                        e.setMaxCubeCount(Cubes.getMaxCubeCount());
                        EventBus.getDefault().post(e);
                        pointCloudManager.updateCallbackBufferAndSwap(xyzIj.xyz, xyzIj.xyzCount);
                    }
                }

                @Override
                public void onTangoEvent(TangoEvent event) {

                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isConnected) {
            glView.disconnectCamera();
            tango.disconnect();
            isConnected = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isConnected && isPermissionGranted) {
            startAugmentedreality();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        renderer.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

}
