package de.stetro.master.masterprototype.rendering.td;


import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.NPrism;

import java.util.Stack;

import de.stetro.master.masterprototype.rendering.Materials;

public class WayPoints extends Object3D {

    private static final int MAX_WAYPOINT_COUNT = 4;
    private int wayPointCount = 0;
    private Stack<Vector3> points;

    public WayPoints() {
        Material material = Materials.generateBlueMaterial();
        for (int i = 0; i < MAX_WAYPOINT_COUNT; i++) {
            NPrism p = new NPrism(4, 0.05, 0.1);
            p.setMaterial(material);
            p.setVisible(false);
            addChild(p);
        }
        points = new Stack<>();
    }

    public static int getMaxWaypointCount() {
        return MAX_WAYPOINT_COUNT;
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    public void addWayPoint(Vector3 position) {
        Object3D p = getChildAt(wayPointCount);
        p.setPosition(position);
        p.setVisible(true);
        points.add(position);
        wayPointCount++;
    }

    public int getWayPointCount() {
        return wayPointCount;
    }


    public void clear() {
        for (int i = 0; i < MAX_WAYPOINT_COUNT; i++) {
            Object3D cube = getChildAt(i);
            cube.setVisible(false);
        }
        wayPointCount = 0;
        points.clear();
    }

    public Stack<Vector3> getPoints() {
        return points;
    }
}
