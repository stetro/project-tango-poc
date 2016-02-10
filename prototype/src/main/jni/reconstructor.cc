#include "tango-augmented-reality/reconstructor.h"

namespace tango_augmented_reality {

    void Reconstructor::reconstruct() {
        mesh_.clear();
        if (points.size() < 3) return;
        Plane plane = detectPlane();
        std::vector <glm::vec2> projection = project(plane, ransac_best_supporting_points);

        
        std::vector <glm::vec3> back_projection = project(plane, projection);
    }

    std::vector <glm::vec2> Reconstructor::project(Plane plane, std::vector <glm::vec3> &points) {
        std::vector <glm::vec2> result;
        for (int i = 0; i < points.size(); ++i) {
            glm::vec3 point = points[i];
            point = point - plane.plane_origin;
            point = plane.plane_z_rotation * point;
            result.push_back(glm::vec2(point[0], point[1]));
        }
        return result;
    }

    std::vector <glm::vec3> Reconstructor::project(Plane plane, std::vector <glm::vec2> &points) {
        std::vector <glm::vec3> result;
        for (int i = 0; i < points.size(); ++i) {
            glm::vec3 point = glm::vec3(points[i][0], points[i][1], 0.0);
            point = plane.inverse_plane_z_rotation * point;
            point = point + plane.plane_origin;
            result.push_back(point);
        }
        return result;
    }

    Plane Reconstructor::detectPlane() {
        int best_support = 0;
        Plane result;

        int iterations = ransac_iterations;
        while (iterations > 0) {
            iterations--;
            // 1. pick 3 random points
            int *selected_index = ransacPickThreeRandomPoints();
            // 2. estimate plane from picked points
            Plane plane = Plane::calculatePlane(points[selected_index[0]],
                                                points[selected_index[1]],
                                                points[selected_index[2]]);
            free(selected_index);
            // 3. estimate support for calculated plane
            int support = ransacEstimateSupportingPoints(plane);
            // 4. replace better solutions
            if (best_support < support) {
                best_support = support;
                ransac_best_supporting_points = ransac_supporting_points;
                result = plane;
            }
            // 5. stop if support is already sufficient
            if (best_support >= ransac_sufficient_support) {
                break;
            }
        }
        // 6. apply linear regression to optimize plane with supporting points
        result = ransacApplyLinearRegression(result);
        return result;
    }

    Plane Reconstructor::ransacApplyLinearRegression(Plane plane) {
        // TODO: apply linear regression!
        return plane;
    }

    int Reconstructor::ransacEstimateSupportingPoints(Plane plane) {
        int support = 0;
        ransac_supporting_points.clear();
        for (int i = 0; i < points.size(); ++i) {
            if (plane.distanceTo(points[i]) < ransac_threshold) {
                support++;
                ransac_supporting_points.push_back(points[i]);
            }
        }
        return support;
    }


    int *Reconstructor::ransacPickThreeRandomPoints() {
        int *selected_index = (int *) malloc(sizeof(int) * 3);
        bool *is_selected = (bool *) malloc(sizeof(bool) * points.size());
        for (int j = 0; j < points.size(); ++j) {
            is_selected[j] = false;
        }
        for (int i = 0; i < 3; ++i) {
            do {
                selected_index[i] = rand() % points.size();
            } while (is_selected[selected_index[i]]);
            is_selected[selected_index[i]] = true;
        }
        free(is_selected);
        return selected_index;
    }

    Plane::Plane(glm::vec3 normal, float distance) :
            normal(normal),
            distance(distance),
            plane_origin(normal * distance),
            plane_z_rotation(glm::rotation(normal, glm::vec3(0, 0, 1))),
            inverse_plane_z_rotation(glm::inverse(glm::rotation(normal, glm::vec3(0, 0, 1)))) { }

    Plane Plane::calculatePlane(glm::vec3 p0, glm::vec3 p1, glm::vec3 p2) {
        // Vector3s
        glm::vec3 a = p1 - p0;
        glm::vec3 b = p2 - p0;
        // cross product -> normal Vector3
        glm::vec3 normal = glm::cross(a, b);
        normal = glm::normalize(normal);
        // distance to origin
        float distance = glm::dot(p0, normal);
        return Plane(normal, distance);
    }

    float Plane::distanceTo(glm::vec3 point) {
        return glm::dot(normal, point) - distance;
    }
}