package de.stetro.master.masterprototype.rendering.td;


import android.content.Context;
import android.view.MotionEvent;

import org.rajawali3d.materials.Material;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Line3D;

import de.greenrobot.event.EventBus;
import de.stetro.master.masterprototype.PointCloudManager;
import de.stetro.master.masterprototype.rendering.Materials;
import de.stetro.master.masterprototype.rendering.PrototypeRenderer;
import de.stetro.master.masterprototype.rendering.event.DebugEvent;

public class TDGame extends PrototypeRenderer {
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

        lineMaterial = Materials.generateBlueMaterial();

    }

    @Override
    public void onTouchEvent(MotionEvent event) {
        synchronized (pointCloudSync) {
            if (hasDepthPointIntersection(event)) {
                Vector3 location = points.intersection;
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
                if (enemies.getEnemyCount() < Enemies.getMaxEnemyCount()) {
                    enemies.addEnemy();
                } else {
                    tdMode = TDMode.DONE;
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
            enemies.move();
        }
    }
}
