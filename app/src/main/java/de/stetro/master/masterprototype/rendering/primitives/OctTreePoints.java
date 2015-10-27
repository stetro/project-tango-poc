package de.stetro.master.masterprototype.rendering.primitives;

import android.util.Log;

import com.projecttango.rajawali.renderables.primitives.Points;

import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.vector.Vector3;

import java.nio.FloatBuffer;

import de.stetro.master.masterprototype.calc.OctTree;


public class OctTreePoints extends Points {

    public static final int MAX_OCT_TREE_DEPTH = 11;
    private final static String tag = OctTreePoints.class.getSimpleName();
    private OctTree octTree;
    private int size = 0;

    public OctTreePoints(int numberOfPoints) {
        super(numberOfPoints, 2.0f);
        octTree = new OctTree(new Vector3(-20, -20, -20), 40, MAX_OCT_TREE_DEPTH);
    }

    public void updatePoints(FloatBuffer floatBuffer, int pointCount, Matrix4 modelMatrix) {
        synchronized (this) {
            floatBuffer.rewind();
            while (floatBuffer.hasRemaining()) {
                Vector3 v = new Vector3(floatBuffer.get(), floatBuffer.get(), floatBuffer.get());
                v.multiply(modelMatrix);
                octTree.put(v);
            }
            size = octTree.getSize();
            FloatBuffer newBuffer = FloatBuffer.allocate(size * 3);
            Log.d(tag, "loaded " + size + " from OctTree");
            octTree.fill(newBuffer);
            super.updatePoints(newBuffer, size);
        }
    }

    public void clear() {
        octTree = new OctTree(new Vector3(-20, -20, -20), 40, MAX_OCT_TREE_DEPTH);
    }

    public int getSize() {
        return size;
    }

    public OctTree getOctTree() {
        return octTree;
    }
}
