package de.stetro.master.construct.calculation.clustering.agglomerative;


import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

public abstract class AgglomerativeCluster {
    protected List<Vector3> points;


    public AgglomerativeCluster(Vector3 p) {
        points = new ArrayList<>();
        this.add(p);
    }

    public abstract void add(List<Vector3> newPoints) ;

    public abstract void add(Vector3 newPoint) ;

    public abstract double distance(AgglomerativeCluster that) ;

    public void merge(AgglomerativeCluster that) {
        this.add(that.points);
    }

    public List<Vector3> getPoints() {
        return points;
    }


}
