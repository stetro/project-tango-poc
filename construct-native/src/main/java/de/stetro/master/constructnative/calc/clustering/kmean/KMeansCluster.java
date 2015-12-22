package de.stetro.master.constructnative.calc.clustering.kmean;


import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

public class KMeansCluster {

    public List<Vector3> points;
    public Vector3 centroid;
    public int id;

    public KMeansCluster(int id) {
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
        System.out.println("[KMeansCluster: " + id + "]");
        System.out.println("[Centroid: " + centroid + "]");
        System.out.println("[Points: \n");
        for (Vector3 p : points) {
            System.out.println(p);
        }
        System.out.println("]");
    }

}