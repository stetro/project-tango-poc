/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.stetro.master.prototype;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.erz.joysticklibrary.JoyStick;

// The main activity of the application which shows debug information and a
// glSurfaceView that renders graphic content.
public class MainActivity extends Activity implements
        View.OnClickListener, SeekBar.OnSeekBarChangeListener, JoyStick.JoyStickListener {

    // This code indicates success.
    private static final int TANGO_SUCCESS = 0;

    // The minimum Tango Core version required from this application.
    private static final int MIN_TANGO_CORE_VERSION = 6804;

    // The package name of Tang Core, used for checking minimum Tango Core version.
    private static final String TANGO_PACKAGE_NAME = "com.projecttango.tango";

    // Tag for debug logging.
    private static final String TAG = MainActivity.class.getSimpleName();

    // The interval at which we'll update our UI debug text in milliseconds.
    // This is the rate at which we query our native wrapper around the tango
    // service for pose and event information.
    private static final int UPDATE_UI_INTERVAL_MS = 1000;
    public static final int DIAMETER_FACTOR = 4;
    public static final double SGIMA_FACTOR = 5.0;
    boolean do_filtering = false;
    private GLSurfaceView mGLView;
    // A flag to check if the Tango Service is connected. This flag avoids the
    // program attempting to disconnect from the service while it is not
    // connected.This is especially important in the onPause() callback for the
    // activity class.
    private boolean mIsConnectedService = false;
    // Screen size for normalizing the touch input for orbiting the render camera.
    private Point mScreenSize = new Point();
    // Handles the debug text UI update loop.
    private Handler mHandler = new Handler();
    // Debug text UI update loop, updating at 10Hz.
    private Runnable mUpdateUiLoopRunnable = new Runnable() {
        public void run() {
            mHandler.postDelayed(this, UPDATE_UI_INTERVAL_MS);
        }
    };
    private ARMode mode = ARMode.POINTCLOUD;
    private GestureDetector detector;
    private TapGestureDetector tapGestureDetector;
    private Button addObjectButton;
    private Button clearButton;

    private int diameter = 5;
    private double sigma = 2.2;
    private SeekBar sigmaSeekBar;
    private SeekBar diameterSeekBar;
    private TextView diameterTextView;
    private TextView sigmaTextView;
    private JoyStick joyStick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Querying screen size, used for computing the normalized touch point.
        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(mScreenSize);

        // Setting content view of this activity and getting the mIsAutoRecovery
        // flag from StartActivity.
        setContentView(R.layout.activity_augmented_reality);

        tapGestureDetector = new TapGestureDetector();
        detector = new GestureDetector(this, tapGestureDetector);

        // Buttons for selecting camera view and Set up button click listeners
        findViewById(R.id.first_person_button).setOnClickListener(this);
        findViewById(R.id.third_person_button).setOnClickListener(this);
        findViewById(R.id.top_down_button).setOnClickListener(this);
        findViewById(R.id.show_occlusion).setOnClickListener(this);
        findViewById(R.id.depth_fullscreen).setOnClickListener(this);
        joyStick = (JoyStick) findViewById(R.id.joystick);
        joyStick.setListener(this);
        addObjectButton = (Button) findViewById(R.id.add_object);
        addObjectButton.setOnClickListener(this);
        changeAddObjectLabel();
        clearButton = (Button) findViewById(R.id.clear_reconstruction);
        clearButton.setVisibility(View.INVISIBLE);
        clearButton.setOnClickListener(this);

        sigmaSeekBar = (SeekBar) findViewById(R.id.sigma_seek_bar);
        sigmaSeekBar.setOnSeekBarChangeListener(this);
        sigmaSeekBar.setProgress((int) (sigma * SGIMA_FACTOR));
        diameterSeekBar = (SeekBar) findViewById(R.id.diameter_seek_bar);
        diameterSeekBar.setOnSeekBarChangeListener(this);
        diameterSeekBar.setProgress(diameter * DIAMETER_FACTOR);
        sigmaTextView = (TextView) findViewById(R.id.sigma_text);
        sigmaTextView.setText(String.valueOf(sigma));
        diameterTextView = (TextView) findViewById(R.id.diameter_text);
        diameterTextView.setText(String.valueOf(diameter));

        ((RadioButton) findViewById(R.id.pointclouds)).setChecked(true);
        Button button = (Button) findViewById(R.id.toggle_filter);
        button.setOnClickListener(this);
        changeFilterButtonLabel(button);

        // Button to reset motion tracking
        Button mMotionReset = (Button) findViewById(R.id.resetmotion);

        // OpenGL view where all of the graphics are drawn
        mGLView = (GLSurfaceView) findViewById(R.id.gl_surface_view);

        // Configure OpenGL renderer
        mGLView.setEGLContextClientVersion(2);

        // Set up button click listeners
        mMotionReset.setOnClickListener(this);

        // Configure OpenGL renderer. The RENDERMODE_WHEN_DIRTY is set explicitly
        // for reducing the CPU load. The request render function call is triggered
        // by the onTextureAvailable callback from the Tango Service in the native
        // code.
        Renderer mRenderer = new Renderer();
        mGLView.getHolder().setFixedSize(1280 / 2, 720 / 2);
        mGLView.setRenderer(mRenderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        // Check if the Tango Core is out dated.
        if (!CheckTangoCoreVersion(MIN_TANGO_CORE_VERSION)) {
            Toast.makeText(this, "Tango Core out dated, please update in Play Store", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize Tango Service, this function starts the communication
        // between the application and Tango Service.
        // The activity object is used for checking if the API version is outdated.
        TangoJNINative.initialize(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();

        // Setup the configuration for the TangoService.
        TangoJNINative.setupConfig();

        // Connect the onPoseAvailable callback.
        TangoJNINative.connectCallbacks();

        // Connect to Tango Service (returns true on success).
        // Starts Motion Tracking and Area Learning.
        if (TangoJNINative.connect() == TANGO_SUCCESS) {
            mIsConnectedService = true;
        } else {
            // End the activity and let the user know something went wrong.
            Toast.makeText(this, "Connect Tango Service Error", Toast.LENGTH_LONG).show();
            finish();
        }

        // Start the debug text UI update loop.
        mHandler.post(mUpdateUiLoopRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
        TangoJNINative.deleteResources();

        // Disconnect from Tango Service, release all the resources that the app is
        // holding from Tango Service.
        if (mIsConnectedService) {
            TangoJNINative.disconnect();
            mIsConnectedService = false;
        }

        // Stop the debug text UI update loop.
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TangoJNINative.destroyActivity();
    }

    @Override
    public void onClick(View v) {
        // Handle button clicks.
        switch (v.getId()) {
            case R.id.first_person_button:
                TangoJNINative.setCamera(0);
                break;
            case R.id.top_down_button:
                TangoJNINative.setCamera(2);
                break;
            case R.id.third_person_button:
                TangoJNINative.setCamera(1);
                break;
            case R.id.resetmotion:
                TangoJNINative.resetMotionTracking();
                break;
            case R.id.toggle_filter:
                do_filtering = !do_filtering;
                changeFilterButtonLabel((Button) v);
                TangoJNINative.toggleFilter();
                break;
            case R.id.depth_fullscreen:
                TangoJNINative.setDepthFullscreen(((CheckBox) v).isChecked());
                break;
            case R.id.show_occlusion:
                TangoJNINative.setShowOcclusion(((CheckBox) v).isChecked());
                break;
            case R.id.add_object:
                tapGestureDetector.setAddObject();
                changeAddObjectLabel();
                break;
            case R.id.clear_reconstruction:
                TangoJNINative.clearReconstruction();
                break;
            default:
                Log.w(TAG, "Unknown button click");
        }
    }

    private void changeAddObjectLabel() {
        String additionalLabel = tapGestureDetector.isAddObject() ? "(PICKING)" : "";
        addObjectButton.setText(String.format(getString(R.string.add_object), additionalLabel));
    }

    private void changeFilterButtonLabel(Button button) {
        button.setText(String.format(getString(R.string.filter), (do_filtering) ? "ON" : "OFF"));
        if (do_filtering) {
            findViewById(R.id.filter_controls).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.filter_controls).setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Pass the touch event to the native layer for camera control.
        // Single touch to rotate the camera around the device.
        // Two fingers to zoom in and out.
        int pointCount = event.getPointerCount();
        if (pointCount == 1) {
            float normalizedX = event.getX(0) / mScreenSize.x;
            float normalizedY = event.getY(0) / mScreenSize.y;
            TangoJNINative.onTouchEvent(1, event.getActionMasked(), normalizedX, normalizedY, 0.0f, 0.0f);
            tapGestureDetector.setX(event.getX() / mGLView.getWidth());
            tapGestureDetector.setY(event.getY() / mGLView.getHeight());
            detector.onTouchEvent(event);
            changeAddObjectLabel();

        }
        if (pointCount == 2) {
            if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
                int index = event.getActionIndex() == 0 ? 1 : 0;
                float normalizedX = event.getX(index) / mScreenSize.x;
                float normalizedY = event.getY(index) / mScreenSize.y;
                TangoJNINative.onTouchEvent(1, MotionEvent.ACTION_DOWN, normalizedX, normalizedY, 0.0f, 0.0f);
            } else {
                float normalizedX0 = event.getX(0) / mScreenSize.x;
                float normalizedY0 = event.getY(0) / mScreenSize.y;
                float normalizedX1 = event.getX(1) / mScreenSize.x;
                float normalizedY1 = event.getY(1) / mScreenSize.y;
                TangoJNINative.onTouchEvent(2, event.getActionMasked(), normalizedX0, normalizedY0, normalizedX1, normalizedY1);
            }
        }
        return true;
    }

    // Request render on the glSurfaceView. This function is called from the
    // native code, and it is triggered from the onTextureAvailable callback from
    // the Tango Service.
    public void requestRender() {
        mGLView.requestRender();
    }

    private boolean CheckTangoCoreVersion(int minVersion) {
        int versionNumber = 0;
        String packageName = TANGO_PACKAGE_NAME;
        try {
            PackageInfo pi = getApplicationContext().getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
            versionNumber = pi.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return (minVersion <= versionNumber);
    }

    public void onRadioButtonClicked(View view) {
        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.pointclouds:
                mode = ARMode.POINTCLOUD;
                clearButton.setVisibility(View.INVISIBLE);
                break;
            case R.id.tsdf:
                mode = ARMode.TSDF;
                clearButton.setVisibility(View.VISIBLE);
                break;
            case R.id.plane:
                mode = ARMode.PLANE;
                clearButton.setVisibility(View.VISIBLE);
                break;
        }
        Log.i(TAG, "onRadioButtonClicked: mode is now " + mode);
        TangoJNINative.setMode(mode.id());
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.equals(sigmaSeekBar)) {
            sigma = (double) progress / SGIMA_FACTOR;
            if (sigmaTextView != null)
                sigmaTextView.setText(String.valueOf(sigma));
        }
        if (seekBar.equals(diameterSeekBar)) {
            diameter = progress / DIAMETER_FACTOR;
            if (diameterTextView != null)
                diameterTextView.setText(String.valueOf(diameter));
        }
        TangoJNINative.setFilterSettings(diameter, sigma);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onMove(JoyStick joyStick, double angle, double power) {
        TangoJNINative.joyStick(angle, power);
    }

    enum ARMode {
        POINTCLOUD(0), TSDF(1), PLANE(2);

        private final int id;

        ARMode(int id) {
            this.id = id;
        }

        int id() {
            return id;
        }
    }

}
