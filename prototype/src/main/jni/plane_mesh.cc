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

#include "tango-augmented-reality/plane_mesh.h"
#include <tango-gl/shaders.h>

namespace tango_augmented_reality {

    PlaneMesh::PlaneMesh() {
        render_mode_ = GL_TRIANGLES;
        SetShader();

        tree = new ReconstructionOcTree(glm::vec3(-20, -20, -20), 40, 9);
        ReconstructionOcTree a = ReconstructionOcTree(glm::vec3(-4, -4, -4), 8, 2);
        a.addPoint(glm::vec3(0,0,0));
        a.addPoint(glm::vec3(1,0,0));
        a.addPoint(glm::vec3(0,1,0));
        a.addPoint(glm::vec3(0,0,1));
        LOGE("we got %d cluster", a.getClusterCount());
        a.reconstruct();

    }

    void PlaneMesh::addPoints(std::vector<float> vertices, glm::mat4 transformation) {
        int count = vertices.size() / 3;
        for (int i = 0; i < count; ++i) {
            glm::vec4 point(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2], 1);
            point = point * transformation;
            tree->addPoint(glm::vec3(point.x, point.y, point.z));
        }
        LOGE("got %d points into %d clusters", tree->getSize(), tree->getClusterCount());
        tree->reconstruct();
    }

    void PlaneMesh::updateVertices() {
        std::vector <GLfloat> mesh;
        std::vector <glm::vec3> reconstruction = tree->getMesh();
        for (int i = 0; i < reconstruction.size(); ++i) {
            mesh.push_back(reconstruction[i].x);
            mesh.push_back(reconstruction[i].y);
            mesh.push_back(reconstruction[i].z);

        }
        LOGI("Got %d polygons", mesh.size() / 3);
        SetVertices(mesh);
    }

    PlaneMesh::PlaneMesh(GLenum render_mode) {
        render_mode_ = render_mode;
    }

    void PlaneMesh::SetShader() {
        shader_program_ = tango_gl::util::CreateProgram(
                tango_gl::shaders::GetBasicVertexShader().c_str(),
                tango_gl::shaders::GetBasicFragmentShader().c_str());
        if (!shader_program_) {
            LOGE("Could not create program.");
        }
        uniform_mvp_mat_ = glGetUniformLocation(shader_program_, "mvp");
        attrib_vertices_ = glGetAttribLocation(shader_program_, "vertex");
        uniform_color_ = glGetUniformLocation(shader_program_, "color");

        SetColor(1.0, 0.0, 0.0);
        SetAlpha(0.4);
    }

    void PlaneMesh::Render(const glm::mat4 &projection_mat,
                           const glm::mat4 &view_mat) const {
        glUseProgram(shader_program_);
        glm::mat4 model_mat = GetTransformationMatrix();
        glm::mat4 mv_mat = view_mat * model_mat;
        glm::mat4 mvp_mat = projection_mat * mv_mat;
        glUniformMatrix4fv(uniform_mvp_mat_, 1, GL_FALSE, glm::value_ptr(mvp_mat));
        glUniform4f(uniform_color_, red_, green_, blue_, alpha_);

        glEnableVertexAttribArray(attrib_vertices_);

        if (!indices_.empty()) {
            glVertexAttribPointer(attrib_vertices_, 3, GL_FLOAT, GL_FALSE,
                                  3 * sizeof(GLfloat), vertices_.data());
            glDrawElements(render_mode_, indices_.size(), GL_UNSIGNED_SHORT,
                           indices_.data());
        } else {
            glVertexAttribPointer(attrib_vertices_, 3, GL_FLOAT, GL_FALSE,
                                  3 * sizeof(GLfloat), &vertices_[0]);
            glDrawArrays(render_mode_, 0, vertices_.size() / 3);
        }

        glDisableVertexAttribArray(attrib_vertices_);
        glUseProgram(0);
    }
}  // namespace tango_augmented_reality
