package de.stetro.master.masterprototype.rendering.event;


public class DebugEvent {

    private Object debugObject;

    public DebugEvent(Object debugObject) {
        this.debugObject = debugObject;

    }

    public Object getDebugObject() {
        return debugObject;
    }
}

