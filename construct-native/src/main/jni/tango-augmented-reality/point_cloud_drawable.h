

#ifndef MASTERPROTOTYPE_POINT_CLOUD_DRAWABLE_H
#define MASTERPROTOTYPE_POINT_CLOUD_DRAWABLE_H

#include <jni.h>

#include <tango-gl/util.h>

namespace tango_augmented_reality {
    // PointCloudDrawable is responsible for the point cloud rendering.
    class PointCloudDrawable {
    public:
        PointCloudDrawable();
        ~PointCloudDrawable();

        // Update current point cloud data.
        //
        // @param projection_mat: projection matrix from current render camera.
        // @param view_mat: view matrix from current render camera.
        // @param model_mat: model matrix for this point cloud frame.
        // @param vertices: all vertices in this point cloud frame.
        void Render(glm::mat4 projection_mat, glm::mat4 view_mat, glm::mat4 model_mat,
                    const std::vector<float>& vertices);

    private:
        // Vertex buffer of the point cloud geometry.
        GLuint vertex_buffers_;

        // Shader to display point cloud.
        GLuint shader_program_;

        // Handle to vertex attribute value in the shader.
        GLuint vertices_handle_;

        // Handle to the model view projection matrix uniform in the shader.
        GLuint mvp_handle_;
    };
}

#endif //MASTERPROTOTYPE_POINT_CLOUD_DRAWABLE_H
