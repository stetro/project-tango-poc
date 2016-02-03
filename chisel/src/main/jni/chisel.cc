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