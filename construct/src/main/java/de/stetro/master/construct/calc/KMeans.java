package de.stetro.master.construct.calc;


import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;


public class KMeans {


    private List<Vector3> points;
    private List<Cluster> clusters;
    private int clusterCount;

    public KMeans() {
        this.points = new ArrayList<>();
        this.clusters = new ArrayList<>();
    }

    public void init(List<Vector3> points) {
        this.points = points;
        this.clusterCount = (points.size() / 500) + 1;
        
        // create cluster with random centroid
        for (int i = 0; i < clusterCount; i++) {
            Cluster cluster = new Cluster(i);
            Vector3 centroid = points.get((int) (Math.random() * points.size()));
            cluster.setCentroid(centroid);
            clusters.add(cluster);
        }
    }


    //The process to calculate the K Means, with iterating method.
    public void calculate() {
        boolean finish = false;
        int iteration = 0;

        // Add in new data, one at a time, recalculating centroids with each new one.
        while (!finish) {
            //Clear cluster state
            clearClusters();

            List<Vector3> lastCentroids = getCentroids();

            //Assign points to the closer cluster
            assignCluster();

            //Calculate new centroids.
            calculateCentroids();

            iteration++;

            List<Vector3> currentCentroids = getCentroids();

            //Calculates total distance between new and old Centroids
            double distance = 0;
            for (int i = 0; i < lastCentroids.size(); i++) {
                distance += lastCentroids.get(i).distanceTo(currentCentroids.get(i));
            }

            if (distance == 0) {
                finish = true;
            }
        }
    }

    private void clearClusters() {
        for (Cluster cluster : clusters) {
            cluster.clear();
        }
    }

    private List<Vector3> getCentroids() {
        List<Vector3> centroids = new ArrayList<>(clusterCount);
        for (Cluster cluster : clusters) {
            Vector3 aux = cluster.getCentroid();
            Vector3 point = aux.clone();
            centroids.add(point);
        }
        return centroids;
    }

    private void assignCluster() {
        double max = Double.MAX_VALUE;
        double min;
        int cluster = 0;
        double distance;

        for (Vector3 point : points) {
            min = max;
            for (int i = 0; i < clusterCount; i++) {
                Cluster c = clusters.get(i);
                distance = point.distanceTo(c.getCentroid());
                if (distance < min) {
                    min = distance;
                    cluster = i;
                }
            }
            clusters.get(cluster).addPoint(point);
        }
    }

    private void calculateCentroids() {
        for (Cluster cluster : clusters) {
            double sumX = 0;
            double sumY = 0;
            double sumZ = 0;
            List<Vector3> list = cluster.getPoints();
            int n_points = list.size();

            for (Vector3 point : list) {
                sumX += point.x;
                sumY += point.y;
                sumZ += point.z;
            }

            Vector3 centroid = cluster.getCentroid();
            if (n_points > 0) {
                double newX = sumX / n_points;
                double newY = sumY / n_points;
                double newZ = sumZ / n_points;
                centroid.x = newX;
                centroid.y = newY;
                centroid.z = newZ;
            }
        }
    }

    public List<Cluster> getClusters() {
        return clusters;
    }
}