package de.stetro.master.prototype;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;


public class TapGestureDetector extends GestureDetector.SimpleOnGestureListener {
    private boolean addObject = false;
    private float x;
    private float y;

    public boolean isAddObject() {
        return addObject;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    private static final String TAG = "TapGestureDetector";

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.i(TAG, "onSingleTapUp: " + x + " " + y);
        if (addObject) {
            addObject = false;
            TangoJNINative.addObject(x, y);
        } else {
            TangoJNINative.tap();
        }
        return true;
    }

    public void setAddObject() {
        this.addObject = true;
    }
}
