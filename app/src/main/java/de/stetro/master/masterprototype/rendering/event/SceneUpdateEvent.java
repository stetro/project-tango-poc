package de.stetro.master.masterprototype.rendering.event;

public class SceneUpdateEvent {
    private int pointCloundPointsCount;
    private int maxCubeCount;
    private int cubeCount;

    public int getPointCloundPointsCount() {
        return pointCloundPointsCount;
    }

    public int getMaxCubeCount() {
        return maxCubeCount;
    }

    public int getCubeCount() {
        return cubeCount;
    }

    public void setCubeCount(int cubeCount) {
        this.cubeCount = cubeCount;
    }

    public void setMaxCubeCount(int cubeMaxCount) {
        this.maxCubeCount = cubeMaxCount;
    }

    public void setPointCloundPointsCount(int pointCloundPointsCount) {
        this.pointCloundPointsCount = pointCloundPointsCount;
    }
}
