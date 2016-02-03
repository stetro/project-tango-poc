package de.stetro.master.prototype;

import android.view.GestureDetector;
import android.view.MotionEvent;


public class TapGestureDetector extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        TangoJNINative.tap();
        return true;
    }
}
