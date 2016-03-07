#include "tango-augmented-reality/reconstructor.h"

namespace tango_augmented_reality {

    void Reconstructor::reconstruct() {
        if (points.size() < 4) {
            LOGE("exit because only %d points in cluster", points.size());
            return;
        }


        // RANSAC PLANE DETECTION
        Plane plane = detectPlane();

        // ADD LAST SUPPORTING POINTS IF AVAILABLE
        if (last_best_supporting_points.size() > 0) {
            ransac_best_supporting_points.insert(ransac_best_supporting_points.end(),
                                                 last_best_supporting_points.begin(),
                                                 last_best_supporting_points.end());
            last_best_supporting_points.clear();
        }

        if (ransac_best_supporting_points.size() < 4) {
            LOGE("exit because only %d points in supporting points",
                 ransac_best_supporting_points.size());
            return;
        }

        // SCALE POINTS AROUND CENTROID
        scaleAroundCentroid(1.01, ransac_best_supporting_points);

        // PROJECT SUPPORTING POINTS TO 2D
        std::vector <glm::vec2> projection = project(plane, ransac_best_supporting_points);

        // CALCULATE THE CONVEX HULL
        ConvexHull *h = new ConvexHull();
        std::vector <glm::vec2> hull = h->generateConvexHull(projection);
        hull.pop_back();
        if (hull.size() < 3) {
            LOGE("exit because convex hull has only %d points", hull.size());
            return;
        }
        last_best_supporting_points = ransac_best_supporting_points;    // store convex hull for next iteration

        // triangulate convex hull
        std::vector <glm::vec2> mesh_points;
        for (int i = 0; i < hull.size() - 2; i++) {
            mesh_points.push_back(hull[0]);
            mesh_points.push_back(hull[i + 1]);
            mesh_points.push_back(hull[i + 2]);
        }

        // PROJECT MESH POINTS BACK TO 3D
        mesh_.clear();
        std::vector <glm::vec3> back_projection = project(plane, mesh_points);
        for (int l = 0; l < back_projection.size(); ++l) {
            mesh_.push_back(back_projection[l]);
        }
    }

    std::vector <glm::vec2> Reconstructor::project(Plane plane, std::vector <glm::vec3> &points) {
        std::vector <glm::vec2> result;
        for (int i = 0; i < points.size(); ++i) {
            glm::vec3 point = points[i];
            point = point - plane.plane_origin;
            point = plane.plane_z_rotation * point;
            result.push_back(glm::vec2(point.x, point.y));
        }
        return result;
    }

    std::vector <glm::vec3> Reconstructor::project(Plane plane, std::vector <glm::vec2> &points) {
        std::vector <glm::vec3> result;
        for (int i = 0; i < points.size(); ++i) {
            glm::vec3 point = glm::vec3(points[i].x, points[i].y, 0.0);
            point = plane.inverse_plane_z_rotation * point;
            point = point + plane.plane_origin;
            result.push_back(point);
        }
        return result;
    }

    Plane Reconstructor::detectPlane() {
        int best_support = 0;
        Plane result;
        int ransac_sufficient_support_count = ransac_sufficient_support * points.size();

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
            if (best_support >= ransac_sufficient_support_count) {
                break;
            }
        }
        // 6. apply linear regression to optimize plane with supporting points
        result = ransacApplyLinearRegression(result, ransac_supporting_points);
        return result;
    }

    Plane Reconstructor::ransacApplyLinearRegression(Plane plane, std::vector <glm::vec3> &points) {

        // calculate centroid
        glm::vec3 centroid;
        for (int i = 0; i < points.size(); ++i) {
            centroid += points[i];
        }
        centroid = centroid / points.size();

        // calculate covariance matrix
        Eigen::Matrix3f cv;
        for (int i = 0; i < points.size(); ++i) {
            glm::vec3 s = points[i] - centroid;
            cv(0, 0) += s.x * s.x;
            cv(1, 0) += s.x * s.y;
            cv(2, 0) += s.x * s.z;
            cv(0, 1) += s.y * s.x;
            cv(1, 1) += s.y * s.y;
            cv(2, 1) += s.y * s.z;
            cv(0, 2) += s.z * s.x;
            cv(1, 2) += s.z * s.y;
            cv(2, 2) += s.z * s.z;
        }

        // apply eigenvalue decomposition
        Eigen::EigenSolver <Eigen::Matrix3f> es(cv);
        // get eigen vector of smallest eigen value as normal
        int min_index = 0;
        for (int i = 0; i < 3; ++i) {
            if (es.eigenvalues()[i].real() < es.eigenvalues()[i].real()) {
                min_index = i;
            }
        }
        glm::vec3 normal(es.eigenvectors().col(min_index)[0].real(),
                         es.eigenvectors().col(min_index)[1].real(),
                         es.eigenvectors().col(min_index)[2].real());
        normal = glm::normalize(normal);
        plane.normal = normal;
        // new distance is the cross product of normal and centroid
        plane.distance = glm::dot(normal, centroid);
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

    void Reconstructor::reset() {
        last_best_supporting_points.clear();
        mesh_.clear();
        points.clear();
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

    void Reconstructor::scaleAroundCentroid(float scale, std::vector <glm::vec3> &points) {
        glm::vec3 centroid;
        for (int i = 0; i < points.size(); ++i) {
            centroid = centroid + points[i];
        }
        centroid = centroid / points.size();
        for (int i = 0; i < points.size(); ++i) {
            points[i] = ((points[i] - centroid) * 1.01) + centroid;
        }
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