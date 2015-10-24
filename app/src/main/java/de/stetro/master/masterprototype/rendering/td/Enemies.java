package de.stetro.master.masterprototype.rendering.td;


import android.util.Log;

import org.rajawali3d.Object3D;
import org.rajawali3d.curves.CubicBezierCurve3D;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Sphere;

import java.util.Stack;

import de.stetro.master.masterprototype.rendering.Materials;

public class Enemies extends Object3D {

    private static final int MAX_ENEMY_COUNT = 20;
    private static final int SPAWN_INTERVAL = 100;
    public static double SPEED = 0.001;
    private int enemyCount = 0;
    private Stack<Vector3> points;
    private CubicBezierCurve3D bezierPath;
    private double[] positions = new double[MAX_ENEMY_COUNT];
    private boolean allDead = false;
    private int time = 0;

    public Enemies() {
        for (int i = 0; i < MAX_ENEMY_COUNT; i++) {
            positions[i] = 0;
            Enemy enemy = new Enemy();
            enemy.setVisible(false);
            addChild(enemy);
        }
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    private void addEnemy() {
        Enemy enemy = (Enemy) getChildAt(enemyCount);
        enemy.setPosition(points.get(0));
        enemy.setVisible(true);
        enemyCount++;
    }


    public void clear() {
        for (int i = 0; i < MAX_ENEMY_COUNT; i++) {
            Enemy enemy = (Enemy) getChildAt(i);
            enemy.setVisible(false);
            enemy.clear();
            positions[i] = 0;
        }
        enemyCount = 0;
        allDead = false;
        time = 0;
    }

    public void setPoints(Stack<Vector3> points) {
        this.points = points;
        bezierPath = new CubicBezierCurve3D();
        bezierPath.addPoint(points.get(0), points.get(1), points.get(2), points.get(3));
    }

    public void move(TDMode tdMode) {
        time++;
        if ((time - (SPAWN_INTERVAL * enemyCount)) > 0) {
            if (getEnemyCount() < MAX_ENEMY_COUNT) {
                addEnemy();
            } else if (tdMode == TDMode.DONE) {
                SPEED *= 1.5;
                clear();
            }
        }
        boolean anyAlive = false;
        for (int i = 0; i < MAX_ENEMY_COUNT; i++) {
            Object3D childAt = getChildAt(i);
            if (childAt.isVisible()) {
                positions[i] += SPEED;
                Vector3 v = new Vector3();
                bezierPath.calculatePoint(v, positions[i]);
                childAt.setPosition(v);
                if (positions[i] >= 1.0) {
                    childAt.setVisible(false);
                } else {
                    anyAlive = true;
                }
            }
        }
        allDead = !anyAlive;
    }

    public void checkAttack(Sphere forceField) {
        for (int i = 0; i < MAX_ENEMY_COUNT; i++) {
            Enemy enemy = (Enemy) getChildAt(i);
            enemy.attack(forceField);
        }
    }

    public boolean allDead() {
        return allDead;
    }

    public int getEnemyCount() {
        return enemyCount;
    }


    private class Enemy extends Object3D {
        private static final double ATTACK_DAMAGE = 0.4;

        private final Sphere sphere;
        private double life = 1.0;


        public Enemy() {
            sphere = new Sphere(0.03f, 20, 20);
            sphere.setMaterial(Materials.getGreenMaterial());
            addChild(sphere);
        }

        @Override
        public boolean isContainer() {
            return true;
        }

        public void attack(Sphere forceField) {
            if (forceField.getGeometry().getBoundingSphere().intersectsWith(sphere.getGeometry().getBoundingSphere()) && this.isVisible()) {
                life = life - ATTACK_DAMAGE;
                Log.d(Enemy.class.getSimpleName(), "ATTACK !!" + life);
                sphere.setScale(life);
                if (life < 0.0) {
                    this.setVisible(false);
                }
            }
        }

        public void clear() {
            life = 1.0;
            sphere.setScale(life);
        }
    }
}
