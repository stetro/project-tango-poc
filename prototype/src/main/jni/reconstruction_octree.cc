//
// Created by stetro on 09.02.16.
//

#include "tango-augmented-reality/reconstruction_octree.h"


namespace tango_augmented_reality {

    ReconstructionOcTree::ReconstructionOcTree(glm::vec3 position, double range, int depth) {
        position_ = position_;
        range_ = range;
        halfRange_ = range / 2;
        depth_ = depth;
        children = (ReconstructionOcTree **) malloc(sizeof(ReconstructionOcTree *) * 8);
        for (int i = 0; i < 8; ++i) {
            is_available[i] = false;
        }
        if(depth_==0){
            reconstructor = new Reconstructor();
        }
    }

    int ReconstructionOcTree::getSize() {
        int size = 0;
        if (depth_ == 0) {
            size = points.size();
        } else {
            for (int i = 0; i < 8; ++i) {
                if (is_available[i]) {
                    size += children[i]->getSize();
                }
            }
        }
        return size;
    }

    void ReconstructionOcTree::addPoint(glm::vec3 point) {
        if (depth_ == 0) {
            points.push_back(point);
        } else {
            int index = getChildIndex(point);
            if (!is_available[index]) {
                initChild(point, index);
            }
            children[index]->addPoint(point);
        }
    }

    std::vector <glm::vec3> ReconstructionOcTree::getPoints(glm::vec3 location) {
        int index = getChildIndex(location);
        if (depth_ == 0 || !is_available[index]) {
            return points;
        } else {
            return children[index]->getPoints(location);
        }
    }

    int ReconstructionOcTree::getClusterCount() {
        if (depth_ != 0) {
            int size = 0;
            for (int i = 0; i < 8; ++i) {
                if (is_available[i]) {
                    size += children[i]->getClusterCount();
                }
            }
            return size;
        }
        return 1;
    }

    void ReconstructionOcTree::reconstruct() {
        if (depth_ == 0) {
            //TODO: Reconstructor

        } else {
            for (int i = 0; i < 8; ++i) {
                if (is_available[i]) {
                    children[i]->reconstruct();
                }
            }
        }
    }

    std::vector <glm::vec3> ReconstructionOcTree::getMesh() {
        std::vector <glm::vec3> mesh;
        //TODO: Collect Mesh
        return mesh;
    }


    void ReconstructionOcTree::initChild(glm::vec3 location, int index) {
        glm::vec3 childPosition;
        childPosition.x = (location.x > (position_.x + halfRange_))
                          ? (position_.x + halfRange_) : (position_.x);
        childPosition.y = (location.y > (position_.y + halfRange_))
                          ? (position_.y + halfRange_) : (position_.y);
        childPosition.z = (location.z > (position_.z + halfRange_))
                          ? (position_.z + halfRange_) : (position_.z);
        children[index] = new ReconstructionOcTree(childPosition, halfRange_, depth_ - 1);
        is_available[index] = true;
    }


    int ReconstructionOcTree::getChildIndex(glm::vec3 point) {
        if (point.x < position_.x + halfRange_) {
            if (point.y < position_.y + halfRange_) {
                if (point.z < position_.z + halfRange_) { return 0; } else { return 1; }
            } else {
                if (point.z < position_.z + halfRange_) { return 2; } else { return 3; }
            }
        } else {
            if (point.y < position_.y + halfRange_) {
                if (point.z < position_.z + halfRange_) { return 4; } else { return 5; }
            } else {
                if (point.z < position_.z + halfRange_) { return 6; } else { return 7; }
            }
        }
    }

}