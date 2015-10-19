package de.stetro.master.masterprototype.ui.event;


public class NewPlaneEvent {
    private int supportedPoints;

    public NewPlaneEvent(int supportedPoints) {
        this.supportedPoints = supportedPoints;
    }

    public int getSupportedPoints() {
        return supportedPoints;
    }
}
