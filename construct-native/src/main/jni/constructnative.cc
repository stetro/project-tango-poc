//
// Created by stetro on 01.12.15.
//
#include "constructnative.h"

#include <pcl/point_types.h>
#include <pcl/kdtree/kdtree_flann.h>
#include <pcl/kdtree/impl/kdtree_flann.hpp>
#include <pcl/features/normal_3d.h>
#include <pcl/surface/gp3.h>
#include <pcl/filters/voxel_grid.h>
#include <pcl/surface/simplification_remove_unused_vertices.h>

namespace constructnative {

    void voxelGridDownSampling(const pcl::PointCloud<pcl::PointXYZ>::Ptr source,
                               const pcl::PointCloud<pcl::PointXYZ>::Ptr target,
                               float leafSize) {
        pcl::VoxelGrid <pcl::PointXYZ> sor;
        sor.setInputCloud(source);
        sor.setLeafSize(leafSize, leafSize, leafSize);
        sor.filter(*target);
    }

    void estimateNormals(const pcl::PointCloud<pcl::PointXYZ>::Ptr source,
                         const pcl::PointCloud<pcl::PointNormal>::Ptr target,
                         int kSearch) {
        pcl::NormalEstimation <pcl::PointXYZ, pcl::Normal> n;
        pcl::PointCloud<pcl::Normal>::Ptr normals(new pcl::PointCloud <pcl::Normal>);
        pcl::search::KdTree<pcl::PointXYZ>::Ptr tree(new pcl::search::KdTree <pcl::PointXYZ>);
        tree->setInputCloud(source);
        n.setInputCloud(source);
        n.setSearchMethod(tree);
        n.setKSearch(kSearch);
        n.compute(*normals);
        pcl::concatenateFields(*source, *normals, *target);
    }

    pcl::PolygonMesh greedyTriangulationReconstruction(
            const pcl::PointCloud<pcl::PointNormal>::Ptr source) {
        pcl::PolygonMesh triangles;
        pcl::search::KdTree<pcl::PointNormal>::Ptr tree(new pcl::search::KdTree <pcl::PointNormal>);
        tree->setInputCloud(source);
        pcl::GreedyProjectionTriangulation <pcl::PointNormal> gp3;
        gp3.setSearchRadius(0.2);
        gp3.setMu(3.0);
        gp3.setMaximumNearestNeighbors(100);
        gp3.setMaximumSurfaceAngle(M_PI);
        gp3.setMinimumAngle(M_PI / 10);
        gp3.setMaximumAngle(2 * M_PI / 3.0);
        gp3.setNormalConsistency(false);
        gp3.setConsistentVertexOrdering(true);
        gp3.setInputCloud(source);
        gp3.setSearchMethod(tree);
        gp3.reconstruct(triangles);
        return triangles;
    }

    int verticesToPointCloud(jfloatArray vertices,
                             const pcl::PointCloud<pcl::PointXYZ>::Ptr cloud, JNIEnv *env) {
        int vertexCount = env->GetArrayLength(vertices) / 3;
        jfloat *verticesData = env->GetFloatArrayElements(vertices, NULL);
        cloud->points.resize(vertexCount);
        for (int i = 0; i < vertexCount; ++i) {
            pcl::PointXYZ point;
            point.x = verticesData[i * 3];
            point.y = verticesData[i * 3 + 1];
            point.z = verticesData[i * 3 + 2];
            cloud->points[i] = point;
        }
        return vertexCount;
    }

    jfloatArray polygonMeshToVertices(pcl::PolygonMesh triangles,
                                      const pcl::PointCloud<pcl::PointXYZ>::Ptr filtered_cloud,
                                      JNIEnv *env) {
        int polygonCount = triangles.polygons.size();
        jfloatArray array = env->NewFloatArray(polygonCount * 9);
        for (int i = 0; i < polygonCount; i++) {
            float vertex[9] = {
                    filtered_cloud->points[triangles.polygons[i].vertices[0]].x,
                    filtered_cloud->points[triangles.polygons[i].vertices[0]].y,
                    filtered_cloud->points[triangles.polygons[i].vertices[0]].z,
                    filtered_cloud->points[triangles.polygons[i].vertices[1]].x,
                    filtered_cloud->points[triangles.polygons[i].vertices[1]].y,
                    filtered_cloud->points[triangles.polygons[i].vertices[1]].z,
                    filtered_cloud->points[triangles.polygons[i].vertices[2]].x,
                    filtered_cloud->points[triangles.polygons[i].vertices[2]].y,
                    filtered_cloud->points[triangles.polygons[i].vertices[2]].z
            };
            env->SetFloatArrayRegion(array, i * 9, 9, vertex);
        }
        return array;
    }

    jfloatArray Application::reconstruct(JNIEnv *env, jfloatArray vertices) {

        // transform jfloatArray vertices to pcl::PointCloud
        pcl::PointCloud<pcl::PointXYZ>::Ptr cloud(new pcl::PointCloud <pcl::PointXYZ>);
        verticesToPointCloud(vertices, cloud, env);
        LOGE("PointCloud has %d points", cloud->points.size());

        // filter with voxel grid
        pcl::PointCloud<pcl::PointXYZ>::Ptr filtered_cloud(new pcl::PointCloud <pcl::PointXYZ>);
        voxelGridDownSampling(cloud, filtered_cloud, 0.025f);
        LOGE("filtered PointCloud has %d points", filtered_cloud->points.size());

        // Normal estimation
        pcl::PointCloud<pcl::PointNormal>::Ptr filtered_cloud_with_normals(
                new pcl::PointCloud <pcl::PointNormal>);
        estimateNormals(filtered_cloud, filtered_cloud_with_normals, 10);

        // Triangulate with Greedy Triangulation
        pcl::PolygonMesh triangles = greedyTriangulationReconstruction(filtered_cloud_with_normals);
        LOGE("Reconstructed %d polygons", triangles.polygons.size());

        // transform pcl::PolygonMesh to jfloatArray vertices
        jfloatArray array = polygonMeshToVertices(triangles, filtered_cloud, env);

        return array;
    }

    Application::Application() {
    }

    Application::~Application() {
    }
}