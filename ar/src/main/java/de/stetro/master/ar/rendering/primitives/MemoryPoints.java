package de.stetro.master.ar.rendering.primitives;

import com.projecttango.rajawali.renderables.primitives.Points;

import java.nio.FloatBuffer;

public abstract class MemoryPoints extends Points {
    protected FloatBuffer currentPoints = FloatBuffer.allocate(0);
    protected int currentPointsCount = 0;

    public MemoryPoints(int numberOfPoints) {
        super(numberOfPoints);
    }

    @Override
    public void updatePoints(FloatBuffer pointCloudBuffer, int pointCount) {
        synchronized (this) {
            super.updatePoints(pointCloudBuffer, pointCount);
            pointCloudBuffer.rewind();
            currentPoints = pointCloudBuffer.asReadOnlyBuffer();
            currentPointsCount = pointCount;
        }
    }
}
