package de.stetro.master.constructnative;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends Activity {

    private static final int TANGO_INVALID = -2;
    private static final int TANGO_SUCCESS = 0;
    private static final int REQUEST_PERMISSION_MOTION_TRACKING = 0;
    private static final String MOTION_TRACKING_PERMISSION_ACTION = "android.intent.action.REQUEST_TANGO_PERMISSION";
    private static final String MOTION_TRACKING_PERMISSION = "MOTION_TRACKING_PERMISSION";
    private static final String TAG = "PlaneFittingActivity";
    private GLSurfaceView mGLView;
    private boolean mIsConnectedService = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadAssetLibrary("libflann.so.1.8");
        loadAssetLibrary("libflann_cpp.so.1.8");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        initializeTango();
        setContentView(R.layout.activity_main);
        configureGlSurfaceView();
    }

    private void loadAssetLibrary(String libraryName) {
        try {
            InputStream in = getAssets().open(libraryName);
            File file = new File(Environment.getExternalStorageDirectory(), libraryName);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            System.load(file.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mGLView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    JNIInterface.onTouchEvent(event.getX(), event.getY());
                }
            });
        }
        return super.onTouchEvent(event);
    }

    private void configureGlSurfaceView() {
        mGLView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
        mGLView.setEGLContextClientVersion(2);
        GLSurfaceRenderer mRenderer = new GLSurfaceRenderer(this);
        mGLView.setRenderer(mRenderer);
    }

    private void initializeTango() {
        int status = JNIInterface.tangoInitialize(this);
        if (status != TANGO_SUCCESS) {
            if (status == TANGO_INVALID) {
                Toast.makeText(this, "Tango Service version mis-match", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Tango Service initialize internal error", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
        if (!Util.hasPermission(getApplicationContext(), MOTION_TRACKING_PERMISSION)) {
            getMotionTrackingPermission();
        } else {
            int ret = JNIInterface.tangoSetupAndConnect();
            if (ret != TANGO_SUCCESS) {
                Log.e(TAG, "Failed to set config and connect with code: " + ret);
                finish();
            }
            mIsConnectedService = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
        JNIInterface.freeGLContent();
        if (mIsConnectedService) {
            mIsConnectedService = false;
            JNIInterface.tangoDisconnect();
        }
    }

    private void getMotionTrackingPermission() {
        Intent intent = new Intent();
        intent.setAction(MOTION_TRACKING_PERMISSION_ACTION);
        intent.putExtra("PERMISSIONTYPE", MOTION_TRACKING_PERMISSION);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PERMISSION_MOTION_TRACKING &&
                resultCode == RESULT_CANCELED) {
            mIsConnectedService = false;
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsConnectedService) {
            mIsConnectedService = false;
            JNIInterface.tangoDisconnect();
        }
    }

    public void surfaceCreated() {
        int ret = JNIInterface.initializeGLContent();
        if (ret != TANGO_SUCCESS) {
            Log.e(TAG, "Failed to connect texture with code: " + ret);
        }

        JNIInterface.setRenderDebugPointCloud(true);
    }
}
