
#ifndef TANGO_VIDEO_OVERLAY_DEPTH_DRAWABLE_H
#define TANGO_VIDEO_OVERLAY_DEPTH_DRAWABLE_H

#include "tango-gl/drawable_object.h"

namespace tango_augmented_reality {
    class DepthDrawable : public tango_gl::DrawableObject {
    public:
        DepthDrawable();

        void Render(const glm::mat4 &projection_mat, const glm::mat4 &view_mat) const;

        GLuint GetTextureId() const { return texture_id_; }

        void SetTextureId(GLuint texture_id) { texture_id_ = texture_id; }

    private:
        // This id is populated on construction, and is passed to the tango service.
        GLuint texture_id_;

        GLuint attrib_texture_coords_;
        GLuint uniform_texture_;
        GLuint vertex_buffers_[3];
    };
}  // namespace tango_augmented_reality
#endif  // TANGO_VIDEO_OVERLAY_DEPTH_DRAWABLE_H
