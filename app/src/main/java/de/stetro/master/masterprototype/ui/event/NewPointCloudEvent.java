package de.stetro.master.masterprototype.ui.event;

public class NewPointCloudEvent {
    private int points;

    public NewPointCloudEvent(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }
}
