package de.stetro.master.masterprototype.rendering;

import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;


public class Materials {

    public static Material generateGreenMaterial() {
        Material material = new Material();
        material.setColor(new float[]{0.0f, 1.0f, 0.0f, 1.0f});
        material.useVertexColors(false);
        return material;
    }

    public static Material generateWhiteMaterial() {
        Material white = new Material();
        white.enableLighting(true);
        white.setDiffuseMethod(new DiffuseMethod.Lambert());
        white.setColor(new float[]{1.0f, 1.0f, 1.0f, 1.0f});
        return white;
    }

    public static Material generateRedMaterial() {
        Material red = new Material();
        red.enableLighting(true);
        red.setDiffuseMethod(new DiffuseMethod.Lambert());
        red.setColor(new float[]{1.0f, 0.0f, 0.0f, 1.0f});
        return red;
    }
}
