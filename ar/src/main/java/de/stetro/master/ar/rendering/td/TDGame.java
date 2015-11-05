package de.stetro.master.ar.rendering.td;


import android.content.Context;
import android.view.MotionEvent;

import org.rajawali3d.materials.Material;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Line3D;

import de.greenrobot.event.EventBus;
import de.stetro.master.ar.util.PointCloudManager;
import de.stetro.master.ar.rendering.Materials;
import de.stetro.master.ar.rendering.VRPointCloudRenderer;
import de.stetro.master.ar.rendering.event.DebugEvent;

public class TDGame extends VRPointCloudRenderer {
    private Towers towers;
    private WayPoints wayPoints;
    private TDMode tdMode = TDMode.WAYPOINTS;
    private Material lineMaterial;
    private boolean drawLine;
    private Line3D wayPointLine;
    private Enemies enemies;

    public TDGame(Context context, PointCloudManager pointCloudManager) {
        super(context, pointCloudManager);
    }

    @Override
    protected void initScene() {
        super.initScene();

        towers = new Towers();
        getCurrentScene().addChild(towers);

        wayPoints = new WayPoints();
        getCurrentScene().addChild(wayPoints);

        enemies = new Enemies();
        getCurrentScene().addChild(enemies);

        lineMaterial = Materials.getBlueMaterial();

    }

    @Override
    public void onTouchEvent(MotionEvent event) {
        synchronized (pointCloudSync) {
            if (hasDepthPointIntersection(event)) {
                Vector3 location = points.intersection.clone();
                location.y = location.y + 0.1;
                switch (tdMode) {
                    case WAYPOINTS:
                        wayPoints.addWayPoint(location);
                        if (wayPoints.getWayPointCount() >= WayPoints.getMaxWaypointCount()) {
                            tdMode = TDMode.TOWERS;
                            drawLine = true;
                        }
                        break;
                    case TOWERS:
                        towers.addTower(location);
                        if (towers.getTowerCount() >= Towers.getMaxTowerCount()) {
                            tdMode = TDMode.READY;
                        }
                        break;
                }
                EventBus.getDefault().post(new DebugEvent(tdMode));
            }
            if (tdMode == TDMode.READY) {
                if (enemies.getEnemyCount() == 0) {
                    enemies.setPoints(wayPoints.getPoints());
                }
            }
        }
    }

    @Override
    public void clearContent() {
        towers.clear();
        wayPoints.clear();
        enemies.clear();
        if (wayPointLine != null) {
            wayPointLine.setVisible(false);
        }
        tdMode = TDMode.WAYPOINTS;
        super.clearContent();
    }

    @Override
    protected void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);
        if (drawLine) {
            drawLine = false;
            if (wayPointLine != null) {
                getCurrentScene().removeChild(wayPointLine);
            }
            wayPointLine = new Line3D(wayPoints.getPoints(), 10);
            wayPointLine.setMaterial(lineMaterial);
            getCurrentScene().addChild(wayPointLine);
        }
        if (tdMode.equals(TDMode.READY)) {
            enemies.move(tdMode);
            towers.attack(enemies);
            if (enemies.allDead()) {
                tdMode = TDMode.DONE;
            }
        }
    }
}
