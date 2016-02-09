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

#ifndef TANGO_AUGMENTED_REALITY_PLANE_MESH_H_
#define TANGO_AUGMENTED_REALITY_PLANE_MESH_H_

#include <tango-gl/drawable_object.h>

namespace tango_augmented_reality {
    class PlaneMesh : public tango_gl::DrawableObject {
    public:
        PlaneMesh();

        PlaneMesh(GLenum render_mode);

        void SetShader();

        void Render(const glm::mat4 &projection_mat, const glm::mat4 &view_mat) const;

        void addPoints(std::vector<float> vertices, glm::mat4 transformation);

        void updateVertices();

    protected:

        GLuint uniform_mv_mat_;

    };
}  // namespace tango_augmented_reality
#endif  // TANGO_AUGMENTED_REALITY_PLANE_MESH_H_
