#include "tango-augmented-reality/reconstructor.h"

namespace tango_augmented_reality {

    void Reconstructor::reconstruct() {
        if (points.size() < 4) {
            LOGE("exit because only %d points in cluster", points.size());
            return;
        }

        // ADD LAST CONVEX HULL IF AVAILABLE
        if (last_convex_hull.size() > 0) {
            points.insert(points.end(), last_convex_hull.begin(), last_convex_hull.end());
            points.insert(points.end(), last_convex_hull.begin(), last_convex_hull.end());
        }

        // RANSAC PLANE DETECTION
        Plane plane = detectPlane();
        if (ransac_best_supporting_points.size() < 4) {
            LOGE("exit because only %d points in supporting points",
                 ransac_best_supporting_points.size());
            return;
        }

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
        last_convex_hull = project(plane, hull);    // store convex hull for next iteration

        // CALCULATE DELAUNAY TRIANGULATION
        del_point2d_t *delaunay_points = (del_point2d_t *) malloc(
                sizeof(del_point2d_t) * hull.size());
        for (int i = 0; i < hull.size(); ++i) {
            delaunay_points[i] = {hull[i].x, hull[i].y};
        }
        delaunay2d_t *result = delaunay2d_from(delaunay_points, hull.size());
        tri_delaunay2d_t *tri_result = tri_delaunay2d_from(result);

        // PUSH TRIANGLES TO THE CLUSTERS MESH
        std::vector <glm::vec2> mesh_points;
        for (int k = 0; k < tri_result->num_triangles; ++k) {
            mesh_points.push_back(glm::vec2(tri_result->points[tri_result->tris[k * 3]].x,
                                            tri_result->points[tri_result->tris[k * 3]].y));
            mesh_points.push_back(glm::vec2(tri_result->points[tri_result->tris[k * 3 + 1]].x,
                                            tri_result->points[tri_result->tris[k * 3 + 1]].y));
            mesh_points.push_back(glm::vec2(tri_result->points[tri_result->tris[k * 3 + 2]].x,
                                            tri_result->points[tri_result->tris[k * 3 + 2]].y));
        }

        // FREE MEMORY
        delaunay2d_release(result);
        tri_delaunay2d_release(tri_result);
        free(delaunay_points);

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

    void Reconstructor::reset() {
        last_convex_hull.clear();
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