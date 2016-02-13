#include "tango-augmented-reality/convex_hull.h"


namespace {
    // less_equal operator for std::sort function
    bool less_equal(const glm::vec2 p1, const glm::vec2 p2) {
        return p1.x < p2.x || (p1.x == p2.x && p1.y < p2.y);
    }
}
namespace tango_augmented_reality {

    double ConvexHull::isLeft(glm::vec2 P0, glm::vec2 P1, glm::vec2 P2) {
        return (P1.x - P0.x) * (P2.y - P0.y) - (P2.x - P0.x) * (P1.y - P0.y);
    }

    std::vector <glm::vec2> ConvexHull::generateConvexHull(std::vector < glm::vec2 > &points) {

        int n = points.size(), k = 0;
        std::vector <glm::vec2> hull(2 * n);

        // Sort points lexicographically
        std::sort(points.begin(), points.end(), less_equal);

        // Build lower hull
        for (int i = 0; i < n; ++i) {
            while (k >= 2 && isLeft(hull[k - 2], hull[k - 1], points[i]) <= 0) k--;
            hull[k++] = points[i];
        }

        // Build upper hull
        for (int i = n - 2, t = k + 1; i >= 0; i--) {
            while (k >= t && isLeft(hull[k - 2], hull[k - 1], points[i]) <= 0) k--;
            hull[k++] = points[i];
        }

        hull.resize(k);
        return hull;
    }
}