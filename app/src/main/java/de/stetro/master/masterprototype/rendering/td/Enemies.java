package de.stetro.master.masterprototype.rendering.td;


import org.rajawali3d.Object3D;
import org.rajawali3d.curves.CubicBezierCurve3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Sphere;

import java.util.Stack;

import de.stetro.master.masterprototype.rendering.Materials;

public class Enemies extends Object3D {

    private static final int MAX_ENEMY_COUNT = 20;
    private int enemyCount = 0;
    private Stack<Vector3> points;
    private CubicBezierCurve3D bezierPath;
    private double[] positions = new double[MAX_ENEMY_COUNT];

    public Enemies() {
        Material material = Materials.generateGreenMaterial();
        for (int i = 0; i < MAX_ENEMY_COUNT; i++) {
            positions[i] = 0;
            Sphere p = new Sphere(0.03f, 20, 20);
            p.setMaterial(material);
            p.setVisible(false);
            addChild(p);
        }
    }

    public static int getMaxEnemyCount() {
        return MAX_ENEMY_COUNT;
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    public void addEnemy() {
        Object3D p = getChildAt(enemyCount);
        p.setPosition(points.get(0));
        p.setVisible(true);
        enemyCount++;
    }

    public int getEnemyCount() {
        return enemyCount;
    }


    public void clear() {
        for (int i = 0; i < MAX_ENEMY_COUNT; i++) {
            Object3D cube = getChildAt(i);
            cube.setVisible(false);
            positions[i] = 0;
        }
        enemyCount = 0;
    }

    public void setPoints(Stack<Vector3> points) {
        this.points = points;
        bezierPath = new CubicBezierCurve3D();
        bezierPath.addPoint(points.get(0), points.get(1), points.get(2), points.get(3));
    }

    public void move() {
        for (int i = 0; i < MAX_ENEMY_COUNT; i++) {
            Object3D childAt = getChildAt(i);
            if (childAt.isVisible()) {
                positions[i] += 0.001;
                Vector3 v = new Vector3();
                bezierPath.calculatePoint(v, positions[i]);
                childAt.setPosition(v);
                if (positions[i] >= 1.0) {
                    childAt.setVisible(false);
                }
            }

        }
    }
}
