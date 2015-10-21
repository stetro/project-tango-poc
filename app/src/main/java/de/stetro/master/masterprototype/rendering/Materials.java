package de.stetro.master.masterprototype.rendering;

import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.plugins.DepthMaterialPlugin;


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
        red.setDiffuseMethod(new DiffuseMethod.Toon());
        red.setColor(new float[]{1.0f, 0.0f, 0.0f, 1.0f});
        return red;
    }

    public static Material generateBlueMaterial() {
        Material blue = new Material();
        blue.enableLighting(true);
        blue.setColor(new float[]{0.0f, 0.0f, 1.0f, 1.0f});
        return blue;
    }

    public static Material generateAlphaMaterial() {
        Material alphaRed = new Material();
        alphaRed.addPlugin(new DepthMaterialPlugin());
        alphaRed.setColor(new float[]{0.0f, 0.0f, 0.0f, 0.15f});
        return alphaRed;
    }
}
