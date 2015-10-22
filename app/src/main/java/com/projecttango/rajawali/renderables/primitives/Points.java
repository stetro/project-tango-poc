/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.projecttango.rajawali.renderables.primitives;

import android.opengl.GLES10;
import android.opengl.GLES20;

import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.rajawali3d.Object3D;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.nio.FloatBuffer;

import de.stetro.master.masterprototype.rendering.PointCloudMaterial;

/**
 * A Point primitive for Rajawali.
 * Intended to be contributed and PR'ed to Rajawali.
 */
public class Points extends Object3D {
    public Vector3 intersection;
    private int mMaxNumberofVertices;
    private FloatBuffer currentPoints;
    private int currentPointsCount = 0;

    public Points(int numberOfPoints) {
        super();
        mMaxNumberofVertices = numberOfPoints;
        init(true);
        setMaterial(new PointCloudMaterial());
        setTransparent(true);


    }

    // Initialize the buffers for Points primitive.
    // Since only vertex and Index buffers are used, we only initialize them using setdata call.
    protected void init(boolean createVBOs) {
        float[] vertices = new float[mMaxNumberofVertices * 3];
        int[] indices = new int[mMaxNumberofVertices];
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

    // Update the geometry of the points once new Point Cloud Data is available.
    public void updatePoints(FloatBuffer pointCloudBuffer, int pointCount) {
        synchronized (this) {
            pointCloudBuffer.position(0);
            mGeometry.setNumIndices(pointCount);
            mGeometry.getVertices().position(0);
            mGeometry.changeBufferData(mGeometry.getVertexBufferInfo(), pointCloudBuffer, 0, pointCount * 3);
            pointCloudBuffer.rewind();
            currentPoints = pointCloudBuffer.asReadOnlyBuffer();
            currentPointsCount = pointCount;
        }
    }

    public void preRender() {
        super.preRender();
        setDrawingMode(GLES20.GL_POINTS);
        GLES10.glPointSize(5.0f);
    }

    public boolean intersect(Vector3 startRay, Vector3 endRay, Matrix4 modelMatrix) {
        synchronized (this) {
            Line line = new Line(new Vector3D(startRay.x, startRay.y, startRay.z), new Vector3D(endRay.x, endRay.y, endRay.z));
            double minimumDistance = 0.05;
            this.intersection = null;
            currentPoints.position(0);
            for (int i = 0; i < currentPointsCount; i++) {
                float x = currentPoints.get();
                float y = currentPoints.get();
                float z = currentPoints.get();
                Vector3 p = new Vector3(x, y, z);
                p.project(modelMatrix);
                double distance = line.distance(new Vector3D(p.toArray()));
                if (distance <= minimumDistance) {
                    this.intersection = p;
                    return true;
                }
            }
            return false;
        }
    }

}
