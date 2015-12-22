package de.stetro.master.constructnative.calc;


import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

public class Cluster {

    public List<Vector3> points;
    public Vector3 centroid;
    public int id;

    public Cluster(int id) {
        this.id = id;
        this.points = new ArrayList<>();
        this.centroid = null;
    }

    public List<Vector3> getPoints() {
        return points;
    }

    public void setPoints(List<Vector3> points) {
        this.points = points;
    }

    public void addPoint(Vector3 point) {
        points.add(point);
    }

    public Vector3 getCentroid() {
        return centroid;
    }

    public void setCentroid(Vector3 centroid) {
        this.centroid = centroid;
    }

    public int getId() {
        return id;
    }

    public void clear() {
        points.clear();
    }

    public void plotCluster() {
        System.out.println("[Cluster: " + id + "]");
        System.out.println("[Centroid: " + centroid + "]");
        System.out.println("[Points: \n");
        for (Vector3 p : points) {
            System.out.println(p);
        }
        System.out.println("]");
    }

}