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

public class PointCollection extends Object3D {
    public static final int REFRESH_SECTIONS = 15;
    private static final String tag = PointCollection.class.getSimpleName();
    private final MeshTree meshTree;
    private int mMaxNumberOfVertices;
    private int count = 0;
    private boolean hasNewPolygons;
    private boolean clearCollectionNextRound = false;
    private boolean isCalculating;

    public PointCollection(int numberOfPoints) {
        super();
        mMaxNumberOfVertices = numberOfPoints;
        init(true);
        Material m = new Material();
        m.setColor(Color.GREEN);
        setMaterial(m);
        meshTree = new MeshTree(new Vector3(-20, -20, -20), 40.0, 11, 4);
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
        Vector3[] points = new Vector3[pointCount];
        final Matrix4 transformation = Matrix4.createTranslationMatrix(pose.getPosition()).rotate(pose.getOrientation());
        for (int i = 0; i < pointCount; i++) {
            double x = pointCloudBuffer.get();
            double y = pointCloudBuffer.get();
            double z = pointCloudBuffer.get();
            points[i] = new Vector3(x, y, z).multiply(transformation);
        }
        for (int i = 0; i < REFRESH_SECTIONS; i++) {
            Vector3 random = points[(int) (Math.random() * points.length)];
            meshTree.putPoints(random, points);
        }
        meshTree.updateMesh();
        if (clearCollectionNextRound) {
            clearCollectionNextRound = false;
            meshTree.clear();
            hasNewPolygons = false;
        } else {
            hasNewPolygons = true;
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
        meshTree.   fill(buffer);
        return buffer;
    }

    public MeshTree getMeshTree() {
        hasNewPolygons = false;
        return meshTree;
    }

    public void clear() {
        clearCollectionNextRound = true;
        mGeometry.setNumVertices(0);
        mGeometry.getVertices().clear();
        count = 0;
    }

    public boolean hasNewPolygons() {
        return hasNewPolygons;
    }

    public boolean isCalculating() {
        return isCalculating;
    }

    public void setIsCalculating(boolean isCalculating) {
        this.isCalculating = isCalculating;
    }
}

