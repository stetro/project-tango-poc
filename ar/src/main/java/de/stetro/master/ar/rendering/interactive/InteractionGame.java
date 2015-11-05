package de.stetro.master.ar.rendering.interactive;

import android.content.Context;
import android.view.MotionEvent;

import com.erz.joysticklibrary.JoyStick;

import org.rajawali3d.math.vector.Vector3;

import java.lang.*;

import de.stetro.master.ar.util.PointCloudManager;
import de.stetro.master.ar.rendering.VRPointCloudRenderer;


public class InteractionGame extends VRPointCloudRenderer implements JoyStick.JoyStickListener {

    private Character character;
    private double angle;
    private double power;

    public InteractionGame(Context context, PointCloudManager pointCloudManager) {
        super(context, pointCloudManager);
    }

    @Override
    protected void initScene() {
        super.initScene();
        character = new Character();
        character.setVisible(false);
        getCurrentScene().addChild(character);
    }

    @Override
    public void onTouchEvent(MotionEvent event) {
        synchronized (pointCloudSync) {
            if (hasDepthPointIntersection(event)) {
                Vector3 location = points.intersection.clone();
                location.y = location.y + 0.1;
                character.setPosition(location);
                character.setVisible(true);
            }
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
    }

    @Override
    protected void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);
        character.moveForward(power / 5000.0);
        if (power > 0.00001) {
            character.rotate(Vector3.Axis.Y, ((angle * (180.0 / Math.PI)) - 90.0) / 40);
        }
    }

    @Override
    public void onMove(JoyStick joyStick, double angle, double power) {
        this.angle = angle;
        this.power = power;
    }
}
