//
// Created by stetro on 09.02.16.
//

#include <tango-gl/util.h>
#include <glm/glm.hpp>
#include <glm/ext.hpp>
#include <vector>
#include <vector>

#ifndef MASTERPROTOTYPE_RECONSTRUCTOR_H
#define MASTERPROTOTYPE_RECONSTRUCTOR_H

namespace tango_augmented_reality {

    class Plane {
    public:
        // plane normal (hesse normal form)
        glm::vec3 normal;
        // plane distance from origin (hesse normal form)
        float distance = 0.0;

        // variables for the projection calculation
        glm::vec3 plane_origin;
        glm::quat plane_z_rotation;
        glm::quat inverse_plane_z_rotation;

        Plane(glm::vec3 normal, float distance);

        Plane() { };

        Plane &operator=(const Plane &plane) {
            normal = plane.normal;
            distance = plane.distance;
            plane_origin = plane.plane_origin;
            plane_z_rotation = plane.plane_z_rotation;
            inverse_plane_z_rotation = plane.inverse_plane_z_rotation;
        };

        // calculates the distance between a point and this plane
        float distanceTo(glm::vec3 point);

        // computes the plane model from three points
        static Plane calculatePlane(glm::vec3 p0, glm::vec3 p1, glm::vec3 p2);
    };

    class Reconstructor {
    public:
        // delegated points of the octree
        std::vector <glm::vec3> points;

        // gets the reconstructed mesh
        std::vector <glm::vec3> getMesh() { return mesh_; }

        // triggers the mesh reconstruction from points
        void reconstruct();

    private:
        // the resulting mesh
        std::vector <glm::vec3> mesh_;

        // uses RANSAC to detect a plane model
        Plane detectPlane();

        // project points onto the plane
        std::vector <glm::vec2> project(Plane plane, std::vector <glm::vec3> &points);

        // project points back from the plane
        std::vector <glm::vec3> project(Plane plane, std::vector <glm::vec2> &points);

        // computes the support of the plane against points with ransac_threshold
        int ransacEstimateSupportingPoints(Plane plane);

        // computes the support of the plane against points with ransac_threshold
        int *ransacPickThreeRandomPoints();

        // method to apply linear regression with best supporting points and plane
        Plane ransacApplyLinearRegression(Plane plane);

        // how many random samples we're going to test
        int ransac_iterations = 10;
        // threshold between plane and point to count a point as supporting
        float ransac_threshold = 0.05;
        // amount of points, which should support the plane model to be sufficient
        int ransac_sufficient_support;
        // supporting points of ransac estimation
        std::vector <glm::vec3> ransac_supporting_points;
        // supporting points of best ransac estimation
        std::vector <glm::vec3> ransac_best_supporting_points;

    };

}

#endif