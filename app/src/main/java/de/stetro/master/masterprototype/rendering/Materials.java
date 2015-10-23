package de.stetro.master.masterprototype.rendering;

import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.plugins.DepthMaterialPlugin;


public class Materials {

    public static Material generateGreenMaterial() {
        Material material = new Material();
        material.setDiffuseMethod(new DiffuseMethod.Lambert());
        material.setColor(new float[]{0.0f, 0.8f, 0.0f, 1.0f});
        material.enableLighting(true);
        return material;
    }

    public static Material generateRedMaterial() {
        Material red = new Material();
        red.enableLighting(true);
        red.setDiffuseMethod(new DiffuseMethod.Lambert());
        red.setColor(new float[]{0.8f, 0.0f, 0.0f, 1.0f});
        return red;
    }

    public static Material generateBlueMaterial() {
        Material blue = new Material();
        blue.enableLighting(true);
        blue.setDiffuseMethod(new DiffuseMethod.Lambert());
        blue.setColor(new float[]{0.0f, 0.0f, 0.8f, 1.0f});
        return blue;
    }

}
