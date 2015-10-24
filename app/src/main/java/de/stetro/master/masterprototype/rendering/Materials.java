package de.stetro.master.masterprototype.rendering;

import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;


public class Materials {

    private static Material green;
    private static Material red;
    private static Material blue;
    private static Material transparentRed;

    private static Material createLambertMaterial(float[] color) {
        Material material = new Material();
        material.setDiffuseMethod(new DiffuseMethod.Lambert());
        material.setColor(color);
        material.enableLighting(true);
        return material;
    }

    public static Material getGreenMaterial() {
        if (green == null) {
            float[] color = {0.0f, 0.8f, 0.0f, 1.0f};
            green = createLambertMaterial(color);
        }
        return green;
    }

    public static Material getRedMaterial() {
        if (red == null) {
            float[] color = {0.8f, 0.0f, 0.0f, 1.0f};
            red = createLambertMaterial(color);
        }
        return red;
    }

    public static Material getBlueMaterial() {
        if (blue == null) {
            float[] color = {0.0f, 0.0f, 0.8f, 1.0f};
            blue = createLambertMaterial(color);
        }
        return blue;
    }

    public static Material getTransparentRed() {
        if (transparentRed == null) {
            float[] color = {0.8f, 0.0f, 0.0f, 0.3f};
            transparentRed = createLambertMaterial(color);
        }
        return transparentRed;
    }
}
