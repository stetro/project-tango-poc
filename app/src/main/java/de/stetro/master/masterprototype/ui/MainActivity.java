package de.stetro.master.masterprototype.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoCameraPreview;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoPoseData;

import org.rajawali3d.materials.Material;
import org.rajawali3d.primitives.Cube;
import org.rajawali3d.surface.IRajawaliSurface;
import org.rajawali3d.surface.RajawaliSurfaceView;

import java.util.Collections;
import java.util.List;

import de.stetro.master.masterprototype.R;
import de.stetro.master.masterprototype.rendering.Renderer;
import de.stetro.master.masterprototype.rendering.SensorUpdateListener;

public class MainActivity extends Activity {
    private final String tag = MainActivity.class.getSimpleName();
    private TangoCameraPreview tangoCameraPreview;
    private Tango tango;
    private Renderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // prepare 3D surface
        final RajawaliSurfaceView surface = get3DSurfaceView();
        addContentView(surface, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT));

        // prepare video preview
        tangoCameraPreview = new TangoCameraPreview(this);
        tango = new Tango(this);
        startActivityForResult(Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_MOTION_TRACKING), Tango.TANGO_INTENT_ACTIVITYCODE);
        addContentView(tangoCameraPreview, new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Motion Tracking Permissions Required!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                connectTango();
            }
        }
    }

    private void connectTango() {
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_SMOOTH_POSE, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);
        tango.connect(config);
        tangoCameraPreview.connectToTangoCamera(tango, TangoCameraIntrinsics.TANGO_CAMERA_COLOR);
        List<TangoCoordinateFramePair> framePairs = Collections.singletonList(
                new TangoCoordinateFramePair(
                        TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                        TangoPoseData.COORDINATE_FRAME_DEVICE));
        tango.connectListener(framePairs, new SensorUpdateListener(renderer, tangoCameraPreview));
    }

    @Override
    protected void onPause() {
        super.onPause();
        tango.disconnect();
    }

    @NonNull
    private RajawaliSurfaceView get3DSurfaceView() {
        final RajawaliSurfaceView surface = new RajawaliSurfaceView(this);
        surface.setFrameRate(60.0);
        surface.setRenderMode(IRajawaliSurface.RENDERMODE_WHEN_DIRTY);
        surface.setTransparent(true);
        renderer = new Renderer(this);
        surface.setSurfaceRenderer(renderer);
        return surface;
    }

}
