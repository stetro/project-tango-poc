#ifndef TANGO_AUGMENTED_REALITY_HULL_H_
#define TANGO_AUGMENTED_REALITY_HULL_H_

#include <algorithm>
#include <vector>
#include <tango-gl/util.h>
#include <glm/glm.hpp>
#include <glm/ext.hpp>

namespace tango_augmented_reality {

    class ConvexHull {
    public:

        // applies the convex hull algorithm to determine the convex hull
        std::vector <glm::vec2> generateConvexHull(std::vector <glm::vec2> &points);

        // tests if a point is Left|On|Right of an infinite line.
        double isLeft(glm::vec2 P0, glm::vec2 P1, glm::vec2 P2);

    };
}

#endif