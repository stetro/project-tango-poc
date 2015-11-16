package de.stetro.master.pc.calc;


import org.rajawali3d.math.vector.Vector3;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author stetro
 */
public class OctTree {
    private final double range;
    private final double halfRange;
    private final int depth;
    private final Vector3 position;
    private Vector3 point;
    private OctTree[] children;


    public OctTree(Vector3 position, double range, int depth) {
        this.position = position;
        this.range = range;
        this.halfRange = range / 2;
        this.depth = depth;
        if (depth != 0) {
            children = new OctTree[8];
        }
    }

    public Vector3 getPosition() {
        return position;
    }

    public double getRange() {
        return range;
    }

    public OctTree[] getChildren() {
        return children;
    }

    public Vector3 getPoint() {
        return point;
    }

    public void fill(FloatBuffer buffer) {
        if (depth == 1) {
            for (OctTree child : children) {
                if (child != null) {
                    buffer.put((float) child.point.x);
                    buffer.put((float) child.point.y);
                    buffer.put((float) child.point.z);
                }
            }
        } else {
            for (OctTree child : children) {
                if (child != null) {
                    child.fill(buffer);
                }
            }
        }
    }

    public void put(Vector3 point) {
        if (depth == 0) {
            this.point = point;
        } else if (point.x < position.x || point.x > position.x + range || point.y < position.y || point.y > position.y + range || point.z < position.z || point.z > position.z + range) {
            throw new IllegalArgumentException("Outside of range: " + point.toString() + " does not belong to " + position.toString() + " with range " + range);
        } else {
            if (point.x < position.x + halfRange) {
                if (point.y < position.y + halfRange) {
                    if (point.z < position.z + halfRange) {
                        put(point, 0, position.x, position.y, position.z);
                    } else {
                        put(point, 1, position.x, position.y, position.z + halfRange);
                    }
                } else {
                    if (point.z < position.z + halfRange) {
                        put(point, 2, position.x, position.y + halfRange, position.z);
                    } else {
                        put(point, 3, position.x, position.y + halfRange, position.z + halfRange);
                    }
                }
            } else {
                if (point.y < position.y + halfRange) {
                    if (point.z < position.z + halfRange) {
                        put(point, 4, position.x + halfRange, position.y, position.z);
                    } else {
                        put(point, 5, position.x + halfRange, position.y, position.z + halfRange);
                    }
                } else {
                    if (point.z < position.z + halfRange) {
                        put(point, 6, position.x + halfRange, position.y + halfRange, position.z);
                    } else {
                        put(point, 7, position.x + halfRange, position.y + halfRange, position.z + halfRange);
                    }
                }
            }
        }
    }

    private void put(Vector3 point, int clusterIndex, double x, double y, double z) {
        if (children[clusterIndex] == null) {
            children[clusterIndex] = new OctTree(new Vector3(x, y, z), halfRange, depth - 1);
        }
        children[clusterIndex].put(point);
    }

    private Vector3 findPointNear(Vector3 searchPosition) {
        if (depth == 0) {
            return point;
        } else if (searchPosition.x < position.x || searchPosition.x > position.x + range || searchPosition.y < position.y || searchPosition.y > position.y + range || searchPosition.z < position.z || searchPosition.z > position.z + range) {
            throw new IllegalArgumentException("Outside of range: " + searchPosition.toString() + " does not belong to " + position.toString() + " with range " + range);
        } else {
            if (searchPosition.x < position.x + halfRange) {
                if (searchPosition.y < position.y + halfRange) {
                    if (searchPosition.z < position.z + halfRange) {
                        return findPointNear(searchPosition, 0);
                    } else {
                        return findPointNear(searchPosition, 1);
                    }
                } else {
                    if (searchPosition.z < position.z + halfRange) {
                        return findPointNear(searchPosition, 2);
                    } else {
                        return findPointNear(searchPosition, 3);
                    }
                }
            } else {
                if (searchPosition.y < position.y + halfRange) {
                    if (searchPosition.z < position.z + halfRange) {
                        return findPointNear(searchPosition, 4);
                    } else {
                        return findPointNear(searchPosition, 5);
                    }
                } else {
                    if (searchPosition.z < position.z + halfRange) {
                        return findPointNear(searchPosition, 6);
                    } else {
                        return findPointNear(searchPosition, 7);
                    }
                }
            }
        }
    }

    protected Vector3 findPointNear(Vector3 searchPosition, int clusterIndex) {
        if (children[clusterIndex] == null) {
            return null;
        } else {
            return children[clusterIndex].findPointNear(searchPosition);
        }
    }

    public int getSize() {
        int count = 0;
        if (depth == 1) {
            for (OctTree child : children) {
                if (child != null) {
                    count++;
                }
            }
        } else {
            for (OctTree child : children) {
                if (child != null) {
                    count += child.getSize();
                }
            }
        }
        return count;
    }

    public List<OctTree> getCluster(int depth) {
        List<OctTree> filledOctTrees = new ArrayList<>();
        getCluster(depth, filledOctTrees);
        return filledOctTrees;

    }

    private void getCluster(int depth, List<OctTree> filledOctTrees) {
        if (depth == this.depth) {
            for (OctTree octTree : children) {
                if (octTree != null && octTree.getSize() > 0) {
                    filledOctTrees.add(octTree);
                }
            }
        } else {
            for (OctTree octTree : children) {
                if (octTree != null) {
                    octTree.getCluster(depth, filledOctTrees);
                }
            }
        }
    }

    public List<Vector3> getPointList() {
        List<Vector3> pointList = new LinkedList<>();
        fillPointList(pointList);
        return pointList;
    }

    private void fillPointList(List<Vector3> pointList) {
        if (depth == 1) {
            for (OctTree child : children) {
                if (child != null) {
                    pointList.add(child.point);
                }
            }
        } else {
            for (OctTree child : children) {
                if (child != null) {
                    child.fillPointList(pointList);
                }
            }
        }
    }

    public void clear() {

    }
}
