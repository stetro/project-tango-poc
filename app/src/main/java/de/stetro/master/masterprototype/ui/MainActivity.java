package de.stetro.master.masterprototype.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;

import org.rajawali3d.math.vector.Vector3;

import de.greenrobot.event.EventBus;
import de.stetro.master.masterprototype.R;
import de.stetro.master.masterprototype.rendering.event.CubeUpdateEvent;
import de.stetro.master.masterprototype.rendering.event.DebugEvent;
import de.stetro.master.masterprototype.rendering.event.SceneUpdateEvent;

public class MainActivity extends TangoAppActivity {

    private LinearLayout infoView;
    private TextView cubeInfoTextView;
    private boolean infoViewVisible = true;
    private TextView sceneInfoTextView;
    private TextView debugTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivityForResult(Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_MOTION_TRACKING), Tango.TANGO_INTENT_ACTIVITYCODE);
        setContentView(R.layout.activity_main);
        RelativeLayout wrapperView = (RelativeLayout) findViewById(R.id.wrapper_view);
        wrapperView.addView(glView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        infoView = (LinearLayout) findViewById(R.id.info_view);
        cubeInfoTextView = (TextView) findViewById(R.id.cube_info_text_view);
        sceneInfoTextView = (TextView) findViewById(R.id.scene_info_text_view);
        debugTextView = (TextView) findViewById(R.id.debug_text_view);
        EventBus.getDefault().register(this);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.activity_main_menu_toggle_pointcloud:
                renderer.togglePointCloudVisibility();
                if (renderer.isPointCloudVisible()) {
                    item.setIcon(R.mipmap.ic_visibility_white_24dp);
                } else {
                    item.setIcon(R.mipmap.ic_visibility_off_white_24dp);
                }
                return true;
            case R.id.activity_main_menu_toggle_pointcloud_capturing:
                renderer.togglePointCloudFreeze();
                if (renderer.isPointCloudFreeze()) {
                    item.setIcon(R.mipmap.ic_play_arrow_white_24dp);
                } else {
                    item.setIcon(R.mipmap.ic_pause_white_24dp);
                }
                return true;
            case R.id.activity_main_menu_delete_cubes:
                renderer.deleteCubes();
                return true;
            case R.id.activity_main_menu_info:
                this.toggleInfoSection();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleInfoSection() {
        infoViewVisible = !infoViewVisible;
        if (infoViewVisible) {
            infoView.animate().translationY(0);
        } else {
            infoView.animate().translationY(220 * this.getResources().getDisplayMetrics().density);
        }
    }

    public void onEvent(final SceneUpdateEvent e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder builder = new StringBuilder();
                if (e.getPointCloundPointsCount() > 0) {
                    builder.append("PointCloud points: ").append(e.getPointCloundPointsCount()).append("\n");
                } else {
                    builder.append("no PointCloud points available!").append("\n");
                }
                builder.append("Cubes: ")
                        .append(e.getCubeCount())
                        .append(" / ")
                        .append(e.getMaxCubeCount());
                sceneInfoTextView.setText(builder.toString());
            }
        });
    }

    public void onEvent(CubeUpdateEvent e) {
        StringBuilder builder = new StringBuilder();
        builder.append("touch event @ ")
                .append(e.getTouchX())
                .append(" x ").
                append(e.getTouchY())
                .append("\n");
        builder.append("intersection ray build:\n")
                .append(beautifyVector3(e.getNearIntersectionRayPoint()))
                .append("\n")
                .append(beautifyVector3(e.getFarIntersectionRayPoint()))
                .append("\n");
        if (e.getIntersectionPoint() == null) {
            builder.append("no pointcloud intersection found");
        } else {
            builder.append("intersection @ ")
                    .append(beautifyVector3(e.getIntersectionPoint()));
        }
        cubeInfoTextView.setText(builder.toString());
    }

    public void onEvent(final DebugEvent e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                debugTextView.setText(e.getDebugObject().toString());
            }
        });
    }

    private String beautifyVector3(Vector3 v) {
        return String.format("%+2.4f", v.x) + ", " + String.format("%+2.4f", v.y) + ", " + String.format("%+2.4f", v.z);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
