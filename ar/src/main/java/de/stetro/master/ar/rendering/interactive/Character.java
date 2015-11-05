package de.stetro.master.ar.rendering.interactive;

import android.graphics.Color;

import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.Texture;
import org.rajawali3d.primitives.Sphere;

import de.stetro.master.ar.R;


public class Character extends Object3D {

    public Character() {
        Sphere sphere = new Sphere(0.1f, 20, 20);
        Material characterMaterial = new Material();
        characterMaterial.setColor(Color.RED);
        try {
            characterMaterial.addTexture(new Texture("chessboard", R.drawable.chessboard));
        } catch (ATexture.TextureException e) {
            e.printStackTrace();
        }
        sphere.setMaterial(characterMaterial);
        sphere.moveForward(-0.05);
        addChild(sphere);
        sphere = new Sphere(0.1f, 20, 20);
        sphere.setMaterial(characterMaterial);
        sphere.moveForward(0.05);
        addChild(sphere);
    }

    @Override
    public boolean isContainer() {
        return true;
    }
}
