package de.stetro.master.pc.rendering;


import android.graphics.Color;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.util.Log;

import com.projecttango.rajawali.Pose;

import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class PointCollection extends Object3D {
    private static final String tag = PointCollection.class.getSimpleName();
    private MeshTree meshTree;
    private int mMaxNumberOfVertices;
    private int count = 0;

    public PointCollection(int numberOfPoints) {
        super();
        mMaxNumberOfVertices = numberOfPoints;
        init(true);
        Material m = new Material();
        m.setColor(Color.GREEN);
        setMaterial(m);
        meshTree = new MeshTree(new Vector3(-20, -20, -20), 40.0, 11, 5);
    }

    protected void init(boolean createVBOs) {
        float[] vertices = new float[mMaxNumberOfVertices * 3];
        int[] indices = new int[mMaxNumberOfVertices];
        for (int i = 0; i < indices.length; ++i) {
            indices[i] = i;
        }
        setData(vertices, GLES20.GL_STATIC_DRAW,
                null, GLES20.GL_STATIC_DRAW,
                null, GLES20.GL_STATIC_DRAW,
                null, GLES20.GL_STATIC_DRAW,
                indices, GLES20.GL_STATIC_DRAW,
                true);
    }

    public void updatePoints(FloatBuffer pointCloudBuffer, int pointCount, Pose pose) {
        pointCloudBuffer.position(0);
        long startTime = System.currentTimeMillis();
        List<Vector3> points = new ArrayList<>();
        Matrix4 transformation = Matrix4.createTranslationMatrix(pose.getPosition()).rotate(pose.getOrientation());
        for (int i = 0; i < pointCount; i++) {
            double x = pointCloudBuffer.get();
            double y = pointCloudBuffer.get();
             double z = pointCloudBuffer.get();
            points.add(new Vector3(x, y, z).multiply(transformation));
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        Log.d(tag, "first converting took " + estimatedTime);
        for (int i = 0; i < 3; i++) {
            Vector3 random = points.get((int) (Math.random() * points.size()));
            meshTree.putPoints(random, points);
            estimatedTime = System.currentTimeMillis() - startTime;
            Log.d(tag, "added points to random point number " + i + " took until now " + estimatedTime);
        }
    }

    public void preRender() {
        super.preRender();
        setDrawingMode(GLES20.GL_POINTS);
        GLES10.glPointSize(3.0f);
    }

    public int getCount() {
        return count;
    }

    public FloatBuffer getBuffer() {
        int size = meshTree.getSize();
        FloatBuffer buffer = FloatBuffer.allocate(size * 3);
        meshTree.fill(buffer);
        return buffer;
    }

    public MeshTree getMeshTree() {
        return meshTree;
    }

    public void clear() {
        meshTree = new MeshTree(new Vector3(-20, -20, -20), 40.0, 12, 5);
        mGeometry.setNumVertices(0);
        mGeometry.getVertices().clear();
        count = 0;
    }
}

