#include "tango-augmented-reality/reconstructor.h"

namespace tango_augmented_reality {

    void Reconstructor::reconstruct() {

        mesh_.clear();

        if (points.size() > 2) {
            mesh_.push_back(points[0]);
            mesh_.push_back(points[1]);
            mesh_.push_back(points[2]);
        }
    }

}