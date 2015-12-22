package de.stetro.master.construct.calc.clustering.agglomerative;


import org.rajawali3d.math.vector.Vector3;

import java.util.List;

public class EuclideanCentroidCluster extends AgglomerativeCluster {
    private Vector3 centroid;

    public EuclideanCentroidCluster(Vector3 p) {
        super(p);
    }

    @Override
    public void add(List<Vector3> newPoints) {
        for (Vector3 newPoint : newPoints) {
            this.add(newPoint);
        }
    }

    @Override
    public void add(Vector3 newPoint) {
        recalculateCentroid(newPoint);
        points.add(newPoint);
    }

    @Override
    public double distance(AgglomerativeCluster that) {
        EuclideanCentroidCluster thatCluster = (EuclideanCentroidCluster) that;
        return this.centroid.distanceTo(thatCluster.centroid);
    }

    private void recalculateCentroid(Vector3 newPoint) {
        Vector3 newCentroid = centroid.clone().multiply(points.size());
        newCentroid.add(newPoint);
        centroid = newCentroid.divide(points.size() + 1);
    }

}
