package de.stetro.master.masterprototype.rendering.td;


import org.rajawali3d.Object3D;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.RectangularPrism;
import org.rajawali3d.primitives.Sphere;

import de.stetro.master.masterprototype.rendering.Materials;

public class Towers extends Object3D {

    public static final int MAX_TOWER_COUNT = 3;

    private int towerCount = 0;

    public Towers() {
        for (int i = 0; i < MAX_TOWER_COUNT; i++) {
            Tower tower = new Tower();
            tower.setVisible(false);
            addChild(tower);
        }
    }

    public static int getMaxTowerCount() {
        return MAX_TOWER_COUNT;
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    public void addTower(Vector3 intersection) {
        Tower tower = (Tower) getChildAt(towerCount);
        tower.setVisible(true);
        tower.setPosition(intersection);
        towerCount++;
    }

    public void attack(Enemies enemies) {
        for (int i = 0; i < MAX_TOWER_COUNT; i++) {
            Tower tower = (Tower) getChildAt(i);
            tower.attack(enemies);
        }
    }

    public int getTowerCount() {
        return towerCount;
    }

    public void clear() {
        for (int i = 0; i < MAX_TOWER_COUNT; i++) {
            Tower tower = (Tower) getChildAt(i);
            tower.setVisible(false);
            tower.clear();
        }
        towerCount = 0;
    }

    private class Tower extends Object3D {
        private static final int INTERVAL_SIZE = 30;
        private static final int INTERVAL_KEY_FRAME = 100;
        private final RectangularPrism rectangular;
        private final Sphere forceField;
        private double scale = 0.0;
        private int intervalPosition = 0;

        public Tower() {
            rectangular = new RectangularPrism(0.05f, 0.15f, 0.05f);
            rectangular.setMaterial(Materials.getRedMaterial());
            addChild(rectangular);

            forceField = new Sphere(0.2f, 20, 20);
            forceField.setMaterial(Materials.getTransparentRed());
            forceField.setTransparent(true);
            addChild(forceField);
        }

        @Override
        public boolean isContainer() {
            return true;
        }

        public void attack(Enemies enemies) {
            if (intervalPosition > INTERVAL_KEY_FRAME) {
                scale = (double) (intervalPosition - INTERVAL_KEY_FRAME) / (double) INTERVAL_SIZE;
            } else {
                scale = 0.00;
            }
            forceField.setScale(scale);
            if (scale > 1.0) {
                intervalPosition = 0;
                enemies.checkAttack(forceField);
            }
            intervalPosition++;
        }

        public void clear() {
            intervalPosition = 0;
            scale = 0.0;
        }

    }
}
