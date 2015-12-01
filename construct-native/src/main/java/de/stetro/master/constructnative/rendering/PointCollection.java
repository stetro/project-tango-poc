package de.stetro.master.constructnative.rendering;


import android.graphics.Color;
import android.opengl.GLES10;
import android.opengl.GLES20;

import com.projecttango.rajawali.Pose;

import org.rajawali3d.Object3D;
import org.rajawali3d.materials.Material;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.nio.FloatBuffer;

import de.stetro.master.constructnative.calc.OctTree;

public class PointCollection extends Object3D {
    private OctTree octTree;
    private int mMaxNumberOfVertices;
    private float pointSize;
    private int count = 0;

    public PointCollection(int numberOfPoints, float pointSize) {
        super();
        mMaxNumberOfVertices = numberOfPoints;
        this.pointSize = pointSize;
        init(true);
        Material m = new Material();
        m.setColor(Color.GREEN);
        setMaterial(m);
        octTree = new OctTree(new Vector3(-20, -20, -20), 40.0, 11);
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
        if (count + pointCount < mMaxNumberOfVertices) {
            pointCloudBuffer.position(0);
            FloatBuffer transformedPoints = FloatBuffer.allocate(pointCount * 3);
            for (int i = 0; i < pointCount; i++) {
                double x = pointCloudBuffer.get();
                double y = pointCloudBuffer.get();
                double z = pointCloudBuffer.get();
                Vector3 v = new Vector3(x, y, z);
                Matrix4 transformation = Matrix4.createTranslationMatrix(pose.getPosition()).rotate(pose.getOrientation());
                v.multiply(transformation);
                transformedPoints.put((float) v.x);
                transformedPoints.put((float) v.y);
                transformedPoints.put((float) v.z);
                octTree.put(v);
            }

            mGeometry.setNumIndices(pointCount + count);
            mGeometry.getVertices().position(0);
            mGeometry.changeBufferData(mGeometry.getVertexBufferInfo(), transformedPoints, count * 3, pointCount * 3);
            count += pointCount;
        }
    }

    public void preRender() {
        super.preRender();
        setDrawingMode(GLES20.GL_POINTS);
        GLES10.glPointSize(pointSize);
    }

    public int getCount() {
        return count;
    }

    public FloatBuffer getBuffer() {
        int size = octTree.getSize();
        FloatBuffer buffer = FloatBuffer.allocate(size * 3);
        octTree.fill(buffer);
        return buffer;
    }

    public OctTree getOctTree() {
        return octTree;
    }

    public void clear() {
        octTree = new OctTree(new Vector3(-20, -20, -20), 40.0, 12);
        mGeometry.setNumVertices(0);
        mGeometry.getVertices().clear();
        count = 0;
    }
}

