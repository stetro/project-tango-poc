
package de.stetro.master.chisel;


public class JNIInterface {
    static {
        System.loadLibrary("chisel");
    }

    public static native void addPoints(float[] vertices, float[] transformation);

    public static native float[] getMesh();

    public static native void clear();

    public static native void update();

}
