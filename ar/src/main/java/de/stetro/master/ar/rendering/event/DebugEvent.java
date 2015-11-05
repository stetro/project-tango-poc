package de.stetro.master.ar.rendering.event;


public class DebugEvent {

    private Object debugObject;

    public DebugEvent(Object debugObject) {
        this.debugObject = debugObject;

    }

    public Object getDebugObject() {
        return debugObject;
    }
}

