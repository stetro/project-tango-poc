package de.stetro.master.masterprototype.rendering.td;


import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.RectangularPrism;

import de.stetro.master.masterprototype.rendering.Materials;

public class Towers extends Object3D {

    public static final int MAX_TOWER_COUNT = 3;
    private int towerCount = 0;

    public Towers() {
        Material red = Materials.generateRedMaterial();
        for (int i = 0; i < MAX_TOWER_COUNT; i++) {
            Object3D c = new RectangularPrism(0.05f, 0.15f, 0.05f);
            c.setMaterial(red);
            c.setVisible(false);
            addChild(c);
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
        Object3D tower = getChildAt(towerCount);
        tower.setVisible(true);
        tower.setPosition(intersection);
        towerCount++;
    }

    public int getTowerCount() {
        return towerCount;
    }

    public void clear() {
        for (int i = 0; i < MAX_TOWER_COUNT; i++) {
            Object3D cube = getChildAt(i);
            cube.setVisible(false);
        }
        towerCount = 0;
    }
}
