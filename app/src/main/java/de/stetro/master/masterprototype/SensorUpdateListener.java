package de.stetro.master.masterprototype;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoCameraPreview;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import de.greenrobot.event.EventBus;
import de.stetro.master.masterprototype.rendering.Renderer;
import de.stetro.master.masterprototype.ui.event.NewPointCloudEvent;

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
        EventBus.getDefault().post(new NewPointCloudEvent(xyzIj.xyzCount));
        renderer.setPointCloud(xyzIj);
    }

    @Override
    public void onTangoEvent(TangoEvent event) {

    }
}
