package de.stetro.master.masterprototype.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.rajawali.ar.TangoRajawaliView;

import java.util.ArrayList;

import de.stetro.master.masterprototype.PointCloudManager;
import de.stetro.master.masterprototype.R;
import de.stetro.master.masterprototype.rendering.PrototypeRenderer;

public class MainActivity extends Activity {
    private TangoRajawaliView glView;
    private Tango tango;
    private boolean isPermissionGranted;
    private boolean isConnected;
    private TangoConfig config;
    private PointCloudManager pointCloudManager;
    private PrototypeRenderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        glView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renderer.onTouchEvent(null);
            }
        });

        startActivityForResult(Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_MOTION_TRACKING), Tango.TANGO_INTENT_ACTIVITYCODE);
        setContentView(R.layout.activity_main);
        RelativeLayout wrapperView = (RelativeLayout) findViewById(R.id.wrapper_view);
        wrapperView.addView(glView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Motion Tracking Permissions Required!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                startAugmentedreality();
                isPermissionGranted = true;
            }
        }
    }

    private void startAugmentedreality() {
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
}
