//
// Created by stetro on 09.02.16.
//

#include <tango-gl/util.h>

#ifndef MASTERPROTOTYPE_RECONSTRUCTOR_H
#define MASTERPROTOTYPE_RECONSTRUCTOR_H

namespace tango_augmented_reality {

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

    };

}

#endif