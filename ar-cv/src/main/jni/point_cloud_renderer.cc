/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

#include "tango-augmented-reality/point_cloud_renderer.h"

#include <tango-gl/conversions.h>
//#include <tango_support_api.h>


namespace tango_augmented_reality {

    namespace {

        const std::string kPointCloudVertexShader =
                "precision mediump float;\n"
                        "attribute vec4 vertex;\n"
                        "uniform mat4 mvp;\n"
                        "varying vec3 v_color;\n"
                        ""
                        "void main() {\n"
                        "  gl_PointSize = 2.0;\n"
                        "  gl_Position =  mvp*vertex;\n"
                        "  "
                        "  v_color = vec3(0.0, 1.0, 0.0);\n"
                        "}\n";
        const std::string kPointCloudFragmentShader =
                "precision mediump float;\n"
                        "varying vec3 v_color;\n"
                        "void main() {\n"
                        "  gl_FragColor = vec4(v_color, 1.0);\n"
                        "}\n";

    }

    PointCloudRenderer::PointCloudRenderer() : debug_colors_(true) {
        opengl_world_T_start_service_ =
                tango_gl::conversions::opengl_world_T_tango_world();

        shader_program_ = tango_gl::util::CreateProgram(
                kPointCloudVertexShader.c_str(), kPointCloudFragmentShader.c_str());

        glGenBuffers(1, &vertex_buffer_);

        mvp_handle_ = glGetUniformLocation(shader_program_, "mvp");
        vertices_handle_ = glGetAttribLocation(shader_program_, "vertex");

        tango_gl::util::CheckGlError("PointCloudRenderer::Construction");
    }

    PointCloudRenderer::~PointCloudRenderer() {
        glDeleteProgram(shader_program_);
        glDeleteBuffers(0, &vertex_buffer_);
    }

    void PointCloudRenderer::Render(const glm::mat4 &projection_T_depth,
                                    const glm::mat4 &start_service_T_depth,
                                    const TangoXYZij *point_cloud) {

        if (!debug_colors_) {
            return;
        }

        glUseProgram(shader_program_);

        const size_t number_of_vertices = point_cloud->xyz_count;

        glBindBuffer(GL_ARRAY_BUFFER, vertex_buffer_);
        glBufferData(GL_ARRAY_BUFFER, sizeof(GLfloat) * 3 * number_of_vertices,
                     point_cloud->xyz[0], GL_STATIC_DRAW);

        const glm::mat4 depth_T_opengl =
                glm::inverse(opengl_world_T_start_service_ * start_service_T_depth);


        glUniformMatrix4fv(mvp_handle_, 1, GL_FALSE, glm::value_ptr(projection_T_depth));

        glEnableVertexAttribArray(vertices_handle_);
        glVertexAttribPointer(vertices_handle_, 3, GL_FLOAT, GL_FALSE, 0, nullptr);

        glDrawArrays(GL_POINTS, 0, number_of_vertices);

        glDisableVertexAttribArray(vertices_handle_);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glUseProgram(0);
        tango_gl::util::CheckGlError("PointCloudRenderer::Render");
    }

}