package de.stetro.master.construct.rendering;


import android.graphics.Color;

import org.rajawali3d.Object3D;
import org.rajawali3d.math.vector.Vector3;

import java.util.Stack;

public class Polygon extends Object3D {
    private Stack<Vector3> mPoints;


    public Polygon(Stack<Vector3> mPoints) {
        super();
        this.mPoints = mPoints;
        init();
    }

    private void init() {
        setDoubleSided(true);

        int numVertices = mPoints.size();

        float[] vertices = new float[numVertices * 3];
        float[] textureCoords = new float[numVertices * 2];
        float[] normals = new float[numVertices * 3];

        int[] indices = new int[numVertices];


        for (int i = 0; i < numVertices; i++) {
            Vector3 point = mPoints.get(i);
            int index = i * 3;
            vertices[index] = (float) point.x;
            vertices[index + 1] = (float) point.y;
            vertices[index + 2] = (float) point.z;
            normals[index] = 0;
            normals[index + 1] = 0;
            normals[index + 2] = 1;
            index = i * 2;
            textureCoords[index] = 0;
            textureCoords[index + 1] = 0;
            indices[i] = (short) i;
        }

        setData(vertices, normals, textureCoords, null, indices, false);
    }

}