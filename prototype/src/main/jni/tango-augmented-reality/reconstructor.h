//
// Created by stetro on 09.02.16.
//

#include <tango-gl/util.h>
#include <glm/glm.hpp>
#include <glm/ext.hpp>
#include <vector>
#include <Eigen/Core>
#include <Eigen/Eigenvalues>

#include "convex_hull.h"

#ifndef MASTERPROTOTYPE_RECONSTRUCTOR_H
#define MASTERPROTOTYPE_RECONSTRUCTOR_H

#define RANSAC_DETECT_PLANES 3

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

        // current 3d convex hull of plane
        std::vector <glm::vec3> points;

        Plane(glm::vec3 normal, float distance);

        Plane() { };

        Plane &operator=(const Plane &plane) {
            normal = plane.normal;
            distance = plane.distance;
            plane_origin = plane.plane_origin;
            plane_z_rotation = plane.plane_z_rotation;
            inverse_plane_z_rotation = plane.inverse_plane_z_rotation;
            points = plane.points;
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

        // gets the count of available points
        int getPointCount();

        // add a point to a plane or the main point pool
        void addPoint(glm::vec3 point);

        // clear points of the main point pool
        void clearPoints();

        // triggers the mesh reconstruction from points
        void reconstruct();

        // resets the reconstructor
        void reset();

        Reconstructor();


    private:
        // the resulting mesh
        std::vector <glm::vec3> mesh_;

        // uses RANSAC to detect a plane model
        Plane detectPlane(std::vector <glm::vec3> &points);

        // project points onto the plane
        std::vector <glm::vec2> project(Plane plane, std::vector <glm::vec3> &points);

        // project points back from the plane
        std::vector <glm::vec3> project(Plane plane, std::vector <glm::vec2> &points);

        // computes the support of the plane against points with ransac_threshold
        int ransacEstimateSupportingPoints(Plane plane, std::vector <glm::vec3> &points);

        // computes the support of the plane against points with ransac_threshold
        int *ransacPickThreeRandomPoints(std::vector < glm::vec3 > &points);

        // method to apply linear regression with best supporting points and plane
        Plane ransacApplyLinearRegression(Plane plane,  std::vector <glm::vec3> &points);

        // scales given points around calculated centroid
        void scaleAroundCentroid(float scale, std::vector <glm::vec3> &points);

        // how many random samples we're going to test
        int ransac_iterations = 8;
        // threshold between plane and point to count a point as supporting
        float ransac_threshold = 0.08;
        // amount of points, which should support the plane model to be sufficient
        float ransac_sufficient_support = 0.33;
        // how many planes per cluster getting detected
        const int ransac_detect_planes = RANSAC_DETECT_PLANES;
        // scale factor to solve the gap problem
        float ransac_scale_planes = 0.08;
        // supporting points of ransac estimation
        std::vector <glm::vec3> ransac_supporting_points;
        // not supporting points of ransac estimation
        std::vector <glm::vec3> ransac_not_supporting_points;
        // supporting points of best ransac estimation
        std::vector <glm::vec3> ransac_best_supporting_points;
        // not supporting points of best ransac estimation
        std::vector <glm::vec3> ransac_best_not_supporting_points;
        // planes per cluster
        std::array<Plane, RANSAC_DETECT_PLANES> planes;
        // available planes
        std::array<bool, RANSAC_DETECT_PLANES> plane_available;

    };

}

#endif