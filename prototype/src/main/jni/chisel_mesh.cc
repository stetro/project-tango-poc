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

#include "tango-augmented-reality/chisel_mesh.h"
#include <tango-gl/shaders.h>

namespace tango_augmented_reality {
    ChiselMesh::ChiselMesh() {
        render_mode_ = GL_TRIANGLES;
        SetShader();

        chunkSize = 8;
        truncationDistScale = 8.0;
        weighting = 0.5;
        enableCarving = true;
        carvingDistance = 0.5;
        chunkResolution = 0.01;
        farClipping = 2.0;
        rayTruncation = 0.5;


        chiselMap = chisel::ChiselPtr(
                new chisel::Chisel(Eigen::Vector3i(chunkSize, chunkSize, chunkSize),
                                   chunkResolution, false));
        // float quadratic, float linear, float constant, float scale
        chisel::TruncatorPtr truncator(new chisel::QuadraticTruncator(0.0019, 0.00152, 0.001504, 8.0));
        chisel::ConstantWeighterPtr weighter(new chisel::ConstantWeighter(weighting));

        chisel::Vec3List centroids;
        projectionIntegrator = chisel::ProjectionIntegrator(truncator, weighter, carvingDistance,
                                                            enableCarving, centroids);
        projectionIntegrator.SetCentroids(chiselMap->GetChunkManager().GetCentroids());
        LOGI("chisel container was created in native environment");
    }

    void ChiselMesh::addPoints(glm::mat4 transformation, TangoCameraIntrinsics intrinsics,
                               TangoXYZij *XYZij) {

        LOGE("Interpolating depth %d x %d", intrinsics.width, intrinsics.height);

        TangoSupportDepthInterpolator *depth_interpolator;
        TangoSupport_createDepthInterpolator(&intrinsics, &depth_interpolator);

        TangoSupportDepthBuffer depth_buffer;
        TangoSupport_initializeDepthBuffer(intrinsics.width, intrinsics.height, &depth_buffer);


        TangoPoseData pose;

        pose.orientation[0] = 0;
        pose.orientation[1] = 0;
        pose.orientation[2] = 0;
        pose.orientation[3] = 0;

        pose.translation[0] = 0;
        pose.translation[1] = 0;
        pose.translation[2] = 0;


        if (TangoSupport_upsampleImageNearest(depth_interpolator, XYZij, &pose, &depth_buffer) !=
            TANGO_SUCCESS) {
            LOGE("Error upsampling the image.");
            return;
        }

        lastDepthImage->SetData(depth_buffer.depths);

        chisel::Transform extrinsic = chisel::Transform();
        for (int j = 0; j < 4; ++j) {
            for (int k = 0; k < 4; ++k) {
                extrinsic(k, j) = transformation[k][j];
            }
        }


        chiselMap->IntegrateDepthScan<float>(
                projectionIntegrator,
                lastDepthImage,
                extrinsic,
                pinHoleCamera
        );

        TangoSupport_freeDepthBuffer(&depth_buffer);
        TangoSupport_freeDepthInterpolator(depth_interpolator);

    }

    void ChiselMesh::init(TangoCameraIntrinsics intrinsics) {

        lastDepthImage.reset(new chisel::DepthImage<float>(intrinsics.width, intrinsics.height));

        chiselIntrinsics.SetFx(intrinsics.fx);
        chiselIntrinsics.SetFy(intrinsics.fy);
        chiselIntrinsics.SetCx(intrinsics.cx);
        chiselIntrinsics.SetCy(intrinsics.cy);

        pinHoleCamera.SetWidth(intrinsics.width);
        pinHoleCamera.SetHeight(intrinsics.height);
        pinHoleCamera.SetNearPlane(0.1);
        pinHoleCamera.SetFarPlane(2.0);
        pinHoleCamera.SetIntrinsics(chiselIntrinsics);

    }

    void ChiselMesh::updateVertices() {
        chiselMap->UpdateMeshes();
        LOGI("Generating Mesh ...");
        chisel::MeshMap meshMap = chiselMap->GetChunkManager().GetAllMeshes();
        LOGI("Map with %d items", meshMap.size());

        std::vector <GLfloat> mesh;
        for (const std::pair <chisel::ChunkID, chisel::MeshPtr> &meshes : meshMap) {
            for (size_t &index: meshes.second->indices) {
                mesh.push_back(meshes.second->vertices[index](0));
                mesh.push_back(meshes.second->vertices[index](1));
                mesh.push_back(meshes.second->vertices[index](2));
            }
        }
        LOGI("Got %d polygons", mesh.size() / 3);

        {
            std::lock_guard <std::mutex> lock(render_mutex);
            SetVertices(mesh);
        }
    }

    void ChiselMesh::clear() {
        std::lock_guard <std::mutex> lock(render_mutex);
        std::vector <GLfloat> mesh;
        SetVertices(mesh);
        chiselMap->Reset();
    }

    ChiselMesh::ChiselMesh(GLenum render_mode) {
        render_mode_ = render_mode;
    }

    void ChiselMesh::SetShader() {
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

    void ChiselMesh::Render(const glm::mat4 &projection_mat,
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
