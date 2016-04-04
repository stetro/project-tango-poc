//
// Created by stetro on 09.02.16.
//

#include <tango-gl/util.h>
#include <vector>
#include "reconstructor.h"

#ifndef MASTERPROTOTYPE_RECONSTRUCTION_OCTREE_H
#define MASTERPROTOTYPE_RECONSTRUCTION_OCTREE_H

namespace tango_augmented_reality {

    class ReconstructionOcTree {
    public:

        ReconstructionOcTree(glm::vec3 position, float range, int depth);

        // get global point count in Octree
        int getSize();

        // counts the filled cluster in Octree
        int getClusterCount();

        // add a single point to the deepest level
        void addPoint(glm::vec3 point);


        // triggers the clusters reconstruction
        void reconstruct();

        // collects the reconstructed mesg from each cluster
        std::vector <glm::vec3> getMesh();

        // instance of a reconstructor for mesh generation
        Reconstructor *reconstructor;

        // removes the current plane reconstruction
        void clear();

    private:
        // size of a cubic node
        float range_;
        // size / 2 of a cubic node
        float halfRange_;
        // depth tree depth of current node
        int depth_;
        // spatial position of node
        glm::vec3 position_;
        // array of child node status
        bool is_available_[8];
        // 8 children of a node
        ReconstructionOcTree **children_;
        // boolean flag if the points got updated
        bool updated;

        // get Octree child index of a given point
        int getChildIndex(glm::vec3 point);

        // initializes an Octree child node at a given location
        void initChild(glm::vec3 location, int index);
    };

}

#endif //MASTERPROTOTYPE_RECONSTRUCTION_OCTREE_H
