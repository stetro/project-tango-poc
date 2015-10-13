package de.stetro.master.masterprototype.rendering;

import android.opengl.GLES20;

import org.rajawali3d.Object3D;

import java.util.ArrayList;
import java.util.List;


public class PointCloud extends Object3D {

    private List<Point> points = new ArrayList<>();

    public PointCloud() {
        setDrawingMode(GLES20.GL_POINTS);
    }

    @Override
    public boolean isVisible() {
        return !(points.size() == 0);
    }

    public void init() {
        final int numParticles = points.size();
        float[] vertices = new float[numParticles * 3];
        float[] normals = new float[numParticles * 3];
        float[] textureCords = new float[numParticles * 2];
        float[] colors = new float[numParticles * 4];
        int[] indices = new int[numParticles];
        int index = 0;
        for (int i = 0; i < numParticles; ++i) {
            index = i * 3;
            vertices[index] = points.get(i).getX();
            vertices[index + 1] = points.get(i).getY();
            vertices[index + 2] = points.get(i).getZ();

            normals[index] = 0;
            normals[index + 1] = 0;
            normals[index + 2] = 1;

            index = i * 2;
            textureCords[index] = 0;
            textureCords[index + 1] = 0;

            index = i * 4;
            colors[index] = 1;
            colors[index + 1] = 0;
            colors[index + 2] = 0;
            colors[index + 3] = 1;

            indices[i] = i;
        }

        setData(vertices, normals, textureCords, colors, indices, false);
    }

    public void add(Point point) {
        points.add(point);
    }


}
