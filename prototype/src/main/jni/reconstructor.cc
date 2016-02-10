#include "tango-augmented-reality/reconstructor.h"

namespace tango_augmented_reality {

    void Reconstructor::reconstruct() {

        mesh_.clear();

        if (points.size() < 4) return;


        Plane *plane = detectPlane();

    }

    Plane *Reconstructor::detectPlane() {
        int best_support = 0;
        Plane *result = nullptr;

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
                result = &plane;
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

    Plane *Reconstructor::ransacApplyLinearRegression(Plane *plane) {
        if (plane == nullptr) {
            return plane;
        }
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

    Plane Plane::calculatePlane(glm::vec3 p0, glm::vec3 p1, glm::vec3 p2) {
        // Vector3s
        glm::vec3 a = p1 - p0;
        glm::vec3 b = p2 - p0;
        // cross product -> normal Vector3
        glm::vec3 normal = glm::cross(a, b);
        glm::normalize(normal);
        // distance to origin
        double distance = glm::dot(p0, normal);
        return Plane(normal, distance);
    }

    double Plane::distanceTo(glm::vec3 point) {
        return glm::dot(normal, point) - distance;
    }
}