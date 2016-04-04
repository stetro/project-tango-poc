
#ifndef TANGO_AUGMENTED_REALITY_PLANE_MESH_H_
#define TANGO_AUGMENTED_REALITY_PLANE_MESH_H_

#include <tango-gl/drawable_object.h>
#include <mutex>

#include "tango-augmented-reality/reconstruction_octree.h"


namespace tango_augmented_reality {
    class PlaneMesh : public tango_gl::DrawableObject {
    public:
        PlaneMesh();

        PlaneMesh(GLenum render_mode);

        void SetShader();

        void Render(const glm::mat4 &projection_mat, const glm::mat4 &view_mat) const;

        void addPoints(glm::mat4 transformation, std::vector <float> &vertices);

        void updateVertices();

        std::mutex render_mutex;

        void clear();

    protected:

        GLuint uniform_mv_mat_;

        ReconstructionOcTree* tree;

    };

}  // namespace tango_augmented_reality
#endif  // TANGO_AUGMENTED_REALITY_PLANE_MESH_H_
