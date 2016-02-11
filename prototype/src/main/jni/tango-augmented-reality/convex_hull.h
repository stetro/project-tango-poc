#ifndef TANGO_AUGMENTED_REALITY_HULL_H_
#define TANGO_AUGMENTED_REALITY_HULL_H_

#include <algorithm>
#include <vector>
#include <tango-gl/util.h>
#include <glm/glm.hpp>
#include <glm/ext.hpp>
#include <poly2tri.h>

namespace tango_augmented_reality {

    class ConvexHull {
    public:

        // applies the convex hull algorithm to determine the convex hull
        std::vector <p2t::Point> generateConvexHull(std::vector <p2t::Point> &points);

        // tests if a point is Left|On|Right of an infinite line.
        double isLeft(p2t::Point P0, p2t::Point P1, p2t::Point P2);

    };
}

#endif