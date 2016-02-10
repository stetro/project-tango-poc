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

#include <Eigen/Core>

#include <open_chisel/Chisel.h>
#include <open_chisel/pointcloud/PointCloud.h>
#include <open_chisel/ProjectionIntegrator.h>
#include <open_chisel/geometry/Geometry.h>
#include <open_chisel/truncation/Truncator.h>
#include <open_chisel/truncation/QuadraticTruncator.h>
#include <open_chisel/truncation/ConstantTruncator.h>
#include <open_chisel/truncation/QuadraticTruncator.h>
#include <open_chisel/weighting/ConstantWeighter.h>
#include <open_chisel/mesh/Mesh.h>

namespace tango_augmented_reality {
    class ChiselMesh : public tango_gl::DrawableObject {
    public:
        ChiselMesh();

        ChiselMesh(GLenum render_mode);

        void SetShader();

        void Render(const glm::mat4 &projection_mat, const glm::mat4 &view_mat) const;

        void addPoints(glm::mat4 transformation, std::vector<float> &vertices);

        void updateVertices();

    protected:
        tango_gl::BoundingBox *bounding_box_;

        GLuint uniform_mv_mat_;

        double truncationDistScale;
        double chunkSize;
        double chunkResolution;
        double weighting;
        double carvingDistance;
        bool enableCarving;
        double farClipping;
        double rayTruncation;

        chisel::ChiselPtr chiselMap;
        chisel::PointCloudPtr lastPointCloud = chisel::PointCloudPtr(new chisel::PointCloud());
        chisel::ProjectionIntegrator projectionIntegrator;
    };
}  // namespace tango_augmented_reality
#endif  // TANGO_AUGMENTED_REALITY_MESH_H_
