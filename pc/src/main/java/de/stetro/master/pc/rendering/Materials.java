package de.stetro.master.pc.rendering;

import org.rajawali3d.materials.Material;


public class Materials {

    private static Material greenPointCloud;
    private static Material bluePointCloud;

    public static Material getGreenPointCloudMaterial() {
        if (greenPointCloud == null) {
            float[] color = {0.0f, 1.0f, 0.0f, 1.0f};
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

}
