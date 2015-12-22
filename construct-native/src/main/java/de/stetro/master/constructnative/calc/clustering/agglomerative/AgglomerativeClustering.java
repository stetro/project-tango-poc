package de.stetro.master.constructnative.calc.clustering.agglomerative;


import org.rajawali3d.math.vector.Vector3;

import java.util.ArrayList;
import java.util.List;


public class AgglomerativeClustering {
    private static final String tag = AgglomerativeClustering.class.getSimpleName();

    public List<AgglomerativeCluster> clusterPoints(List<Vector3> points, double maximumMatchingDistance) {

        // put each point in one cluster
        List<AgglomerativeCluster> agglomerativeClusters = new ArrayList<>();
        for (Vector3 point : points) {
            agglomerativeClusters.add(new EuclideanCentroidCluster(point));
        }

        // run loop until the maximumMatchingDistance is reached
        double matchedClusterDistance = 0.0;
        while (matchedClusterDistance < maximumMatchingDistance) {

            // iterate² over cluster and find shortest cluster distance
            AgglomerativeCluster a = null;
            AgglomerativeCluster b = null;
            double smallestDistance = Double.MAX_VALUE;
            for (AgglomerativeCluster i : agglomerativeClusters) {
                for (AgglomerativeCluster j : agglomerativeClusters) {
                    double distance = i.distance(j);
                    if (distance < smallestDistance && !i.equals(j)) {
                        a = i;
                        b = j;
                        smallestDistance = distance;
                    }
                }
            }

            // if no shortest cluster was found
            if (a == null || b == null) {
                break;
            }
            // merge the shortest distanced AgglomerativeCluster
            matchedClusterDistance = smallestDistance;
            a.merge(b);
            agglomerativeClusters.remove(b);
        }
        return agglomerativeClusters;
    }
}
