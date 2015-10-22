package de.stetro.master.masterprototype.rendering;


import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Cube;

public class Cubes extends Object3D {

    public static final float DEFAULT_SIZE = 0.1f;
    public static final int MAX_CUBE_COUNT = 30;
    private int position = 0;
    private int cubeCount = 0;

    public Cubes() {
        Material red = Materials.generateRedMaterial();
        for (int i = 0; i < MAX_CUBE_COUNT; i++) {
            Cube c = new Cube(DEFAULT_SIZE);
            c.setMaterial(red);
            c.setVisible(false);
            addChild(c);
        }
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    public void addChildCubeAt(Vector3 intersection) {
        Object3D cube = getChildAt(position);
        cube.setVisible(true);
        cube.setPosition(intersection);
        position = (position + 1) % MAX_CUBE_COUNT;
        cubeCount++;
    }

    public int getCubeCount() {
        return cubeCount;
    }

    public static int getMaxCubeCount() {
        return MAX_CUBE_COUNT;
    }

    public void clear() {
        cubeCount = 0;
        for (int i = 0; i < MAX_CUBE_COUNT; i++) {
            Object3D cube = getChildAt(i);
            cube.setVisible(false);
        }
    }
}
