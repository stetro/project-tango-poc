package de.stetro.master.masterprototype.rendering.event;

public class SceneUpdateEvent {
    private int pointCloundPointsCount;
    private int octTreePointCloudPointsCount;

    public int getPointCloundPointsCount() {
        return pointCloundPointsCount;
    }

    public void setPointCloundPointsCount(int pointCloundPointsCount) {
        this.pointCloundPointsCount = pointCloundPointsCount;
    }

    public void setOctTreePointCloudPointsCount(int octTreePointCloudPointsCount) {
        this.octTreePointCloudPointsCount = octTreePointCloudPointsCount;
    }

    public int getOctTreePointCloudPointsCount() {
        return octTreePointCloudPointsCount;
    }
}
