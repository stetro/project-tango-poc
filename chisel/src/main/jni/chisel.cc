//
// Created by stetro on 01.12.15.
//
#include "chisel.h"

#include <open_chisel/geometry/Geometry.h>
#include <open_chisel/truncation/QuadraticTruncator.h>
#include <open_chisel/truncation/ConstantTruncator.h>
#include <open_chisel/truncation/QuadraticTruncator.h>
#include <open_chisel/weighting/ConstantWeighter.h>
#include <open_chisel/mesh/Mesh.h>

#include <Eigen/Core>


namespace chisel {

    void ChiselApplication::addPoints(JNIEnv *env, jfloatArray vertices,
                                      jfloatArray transformation) {
        // move jfloatArray vertices to Chisel PointCloud
        lastPointCloud->Clear();
        int vertexCount = env->GetArrayLength(vertices) / 3;
        LOGI("got %d points from as pointcloud data", vertexCount / 3);
        jfloat *verticesData = env->GetFloatArrayElements(vertices, NULL);
        for (int i = 0; i < vertexCount; ++i) {
            Vec3 vec3(verticesData[i * 3], verticesData[i * 3 + 1], verticesData[i * 3 + 2]);
            lastPointCloud->AddPoint(vec3);
        }

        // move extrisics to a Eigen transformation
        jfloat *transformationData = env->GetFloatArrayElements(transformation, NULL);
        Transform extrinsic = Transform();
        for (int j = 0; j < 4; ++j) {
            for (int k = 0; k < 4; ++k) {
                extrinsic(j, k) = transformationData[j * 4 + k];
            }
        }

        LOGD("%lf %lf %lf %lf", extrinsic(0, 0), extrinsic(1, 0), extrinsic(2, 0), extrinsic(3, 0));
        LOGD("%lf %lf %lf %lf", extrinsic(0, 1), extrinsic(1, 1), extrinsic(2, 1), extrinsic(3, 1));
        LOGD("%lf %lf %lf %lf", extrinsic(0, 2), extrinsic(1, 2), extrinsic(2, 2), extrinsic(3, 2));
        LOGD("%lf %lf %lf %lf", extrinsic(0, 3), extrinsic(1, 3), extrinsic(2, 3), extrinsic(3, 3));

        chiselMap->IntegratePointCloud(
                projectionIntegrator,
                *lastPointCloud,
                extrinsic,
                rayTruncation,
                farClipping
        );

    }

    jfloatArray ChiselApplication::getMesh(JNIEnv * env) {
        LOGD("Getting Mesh ...");
        MeshMap meshMap = chiselMap->GetChunkManager().GetAllMeshes();
        LOGD("Map with %d items", meshMap.size());
        int size = 0;
        for (const std::pair <chisel::ChunkID, chisel::MeshPtr> &meshes : meshMap) {
            size += meshes.second->vertices.size();
        }
        jfloatArray array = env->NewFloatArray(size * 3);
        float points[size * 3];
        int i = 0;
        for (const std::pair <chisel::ChunkID, chisel::MeshPtr> &meshes : meshMap) {
            for (size_t &index: meshes.second->indices) {
                points[i * 3] = meshes.second->vertices[index](0);
                points[i * 3 + 1] = meshes.second->vertices[index](1);
                points[i * 3 + 2] = meshes.second->vertices[index](2);
                i++;
            }
        }
        env->SetFloatArrayRegion(array, 0, size * 3, points);

        return array;
    }

    void ChiselApplication::update(JNIEnv * env) {
        chiselMap->UpdateMeshes();
    }

    void ChiselApplication::clear(JNIEnv * env) {
        chiselMap.reset(new chisel::Chisel(Eigen::Vector3i(chunkSize, chunkSize, chunkSize),
                                           chunkResolution, false));
    }

    ChiselApplication::ChiselApplication() {
        chunkSize = 16;

        truncationDistConst = 0.001504;
        truncationDistLinear = 0.00152;
        truncationDistQuad = 0.0019;
        truncationDistScale = 8.0;

        weighting = 1.0;
        enableCarving = true;
        carvingDistance = 0.5;
        chunkResolution = 0.06;

        farClipping = 2.0;
        rayTruncation = 0.5;

        chiselMap = chisel::ChiselPtr(
                new chisel::Chisel(Eigen::Vector3i(chunkSize, chunkSize, chunkSize),
                                   chunkResolution, false));

        TruncatorPtr truncator(new ConstantTruncator(truncationDistScale));

        ConstantWeighterPtr weighter(new ConstantWeighter(weighting));

        Vec3List centroids;

        projectionIntegrator = ProjectionIntegrator(truncator, weighter, carvingDistance,
                                                    enableCarving, centroids);
        projectionIntegrator.SetCentroids(chiselMap->GetChunkManager().GetCentroids());
        LOGI("ChiselApplication was created in native environment");
    }

    ChiselApplication::~ChiselApplication() {
        LOGI("ChiselApplication was destroyed in native environment");
    }


}