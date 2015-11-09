package de.stetro.master.construct.rendering;

import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;


public class Materials {

    private static Material green;
    private static Material red;
    private static Material blue;
    private static Material transparentRed;
    private static Material greenPointCloud;
    private static Material bluePointCloud;
    private static Material redPointCloud;
    private static Material yellowPointCloud;
    private static Material yellowMaterial;
    private static Material yellow;

    private static Material createLambertMaterial(float[] color) {
        Material material = new Material();
        material.setDiffuseMethod(new DiffuseMethod.Lambert());
        material.setColor(color);
        material.enableLighting(true);
        return material;
    }

    public static Material getGreenMaterial() {
        if (green == null) {
            float[] color = {0.0f, 1.0f, 0.0f, 1.0f};
            green = createLambertMaterial(color);
        }
        return green;
    }

    public static Material getGreenPointCloudMaterial() {
        if (greenPointCloud == null) {
            float[] color = {0.0f, 1.0f, 0.0f, 1.0f};
            greenPointCloud = createMaterial(color);
        }
        return greenPointCloud;
    }

    public static Material getTransparentPointCloudMaterial() {
        if (greenPointCloud == null) {
            float[] color = {0.0f, 0.0f, 0.0f, 0.001f};
            greenPointCloud = createMaterial(color);
        }
        return greenPointCloud;
    }

    public static Material getBluePointCloudMaterial() {
        if (bluePointCloud == null) {
            float[] color = {0.0f, 0.0f, 1.0f, 1.0f};
            bluePointCloud = createMaterial(color);
        }
        return bluePointCloud;
    }

    private static Material createMaterial(float[] color) {
        Material material = new Material();
        material.setColor(color);

        return material;
    }

    public static Material getRedMaterial() {
        if (red == null) {
            float[] color = {0.8f, 0.0f, 0.0f, 0.4f};
            red = createLambertMaterial(color);
        }
        return red;
    }

    public static Material getBlueMaterial() {
        if (blue == null) {
            float[] color = {0.0f, 0.0f, 0.8f, 0.4f};
            blue = createLambertMaterial(color);
        }
        return blue;
    }

    public static Material getRedPointCloudMaterial() {
        if (redPointCloud == null) {
            float[] color = {1.0f, 0.0f, 0.0f, 1.0f};
            redPointCloud = createMaterial(color);
        }
        return redPointCloud;
    }

    public static Material getYellowPointCloudMaterial() {
        if (yellowPointCloud == null) {
            float[] color = {1.0f, 1.0f, 0.0f, 1.0f};
            yellowPointCloud = createMaterial(color);
        }
        return yellowPointCloud;
    }

    public static Material getYellowMaterial() {
        if (yellow == null) {
            float[] color = {1.0f, 1.0f, 0.0f, 0.4f};
            yellow = createLambertMaterial(color);
        }
        return yellow;
    }
}
