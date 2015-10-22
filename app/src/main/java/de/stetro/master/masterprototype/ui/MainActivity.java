package de.stetro.master.masterprototype.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;

import de.stetro.master.masterprototype.R;

public class MainActivity extends TangoAppActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        }
        return super.onOptionsItemSelected(item);
    }
}
