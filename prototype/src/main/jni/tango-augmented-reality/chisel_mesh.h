/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef TANGO_AUGMENTED_REALITY_MESH_H_
#define TANGO_AUGMENTED_REALITY_MESH_H_

#include <tango-gl/bounding_box.h>
#include <tango-gl/drawable_object.h>
#include <tango-gl/segment.h>

namespace tango_augmented_reality {
    class ChiselMesh : public tango_gl::DrawableObject {
    public:
        ChiselMesh();

        ChiselMesh(GLenum render_mode);

        void SetShader();

        void SetShader(bool is_lighting_on);

        void SetBoundingBox();

        void SetLightDirection(const glm::vec3 &light_direction);

        void Render(const glm::mat4 &projection_mat, const glm::mat4 &view_mat) const;

        bool IsIntersecting(const tango_gl::Segment &segment);

    protected:
        tango_gl::BoundingBox *bounding_box_;

        bool is_lighting_on_;

        bool is_bounding_box_on_;

        glm::vec3 light_direction_;

        GLuint uniform_mv_mat_;

        GLuint uniform_light_vec_;
    };
}  // namespace tango_augmented_reality
#endif  // TANGO_AUGMENTED_REALITY_MESH_H_
