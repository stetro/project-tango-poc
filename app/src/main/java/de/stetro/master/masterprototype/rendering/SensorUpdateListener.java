package de.stetro.master.masterprototype.rendering;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoCameraPreview;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

public class SensorUpdateListener implements Tango.OnTangoUpdateListener {
    private Renderer renderer;
    private TangoCameraPreview tangoCameraPreview;

    public SensorUpdateListener(Renderer renderer, TangoCameraPreview tangoCameraPreview) {
        this.renderer = renderer;
        this.tangoCameraPreview = tangoCameraPreview;
    }

    @Override
    public void onPoseAvailable(TangoPoseData pose) {
        renderer.updateCamera(pose);
    }

    @Override
    public void onFrameAvailable(int cameraId) {
        if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
            tangoCameraPreview.onFrameAvailable();
        }
    }

    @Override
    public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
        renderer.setPointCloud(xyzIj);
    }

    @Override
    public void onTangoEvent(TangoEvent event) {

    }
}
