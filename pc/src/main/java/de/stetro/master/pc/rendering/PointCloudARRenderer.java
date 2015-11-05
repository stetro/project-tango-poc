package de.stetro.master.pc.rendering;

import android.content.Context;
import android.view.MotionEvent;

import com.projecttango.rajawali.ar.TangoRajawaliRenderer;

public class PointCloudARRenderer extends TangoRajawaliRenderer {


    public PointCloudARRenderer(Context context) {
        super(context);
    }


    public void capturePoints() {

    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {

    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }
}
