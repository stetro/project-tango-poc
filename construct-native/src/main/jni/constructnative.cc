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

namespace constructnative {

    jfloatArray Application::reconstruct(JNIEnv *env, jfloatArray vertices) {

        int vertexCount = env->GetArrayLength(vertices) / 3;
        jfloat *verticesData = env->GetFloatArrayElements(vertices, NULL);
        LOGE("got vertices : %d", vertexCount);

        // Get the current point cloud data and transform.  This assumes the data has
        // been recently updated on the render thread and does not attempt to update
        // again here.

        pcl::PointCloud<pcl::PointXYZ>::Ptr cloud(new pcl::PointCloud <pcl::PointXYZ>);
        cloud->points.resize(vertexCount);
        for (int i = 0; i < vertexCount; ++i) {
            pcl::PointXYZ point;
            point.x = verticesData[i * 3];
            point.y = verticesData[i * 3 + 1];
            point.z = verticesData[i * 3 + 2];
            cloud->points[i] = point;
        }

        // filter with voxel grid
        pcl::PointCloud<pcl::PointXYZ>::Ptr cloud_filtered(new pcl::PointCloud <pcl::PointXYZ>);
        pcl::VoxelGrid <pcl::PointXYZ> sor;
        sor.setInputCloud(cloud);
        sor.setLeafSize(0.03f, 0.03f, 0.03f);
        sor.filter(*cloud_filtered);

        LOGE("PointCloud has %d points", cloud->points.size());

        // Normal estimation*
        pcl::NormalEstimation <pcl::PointXYZ, pcl::Normal> n;
        pcl::PointCloud<pcl::Normal>::Ptr normals(new pcl::PointCloud <pcl::Normal>);
        pcl::search::KdTree<pcl::PointXYZ>::Ptr tree(new pcl::search::KdTree <pcl::PointXYZ>);
        tree->setInputCloud(cloud_filtered);
        n.setInputCloud(cloud_filtered);
        n.setSearchMethod(tree);
        n.setKSearch(10);
        n.compute(*normals);
        //* normals should not contain the point normals + surface curvatures

        // Concatenate the XYZ and normal fields*
        pcl::PointCloud<pcl::PointNormal>::Ptr cloud_with_normals(
                new pcl::PointCloud <pcl::PointNormal>);
        pcl::concatenateFields(*cloud_filtered, *normals, *cloud_with_normals);
        //* cloud_with_normals = cloud + normals

        // Create search tree*
        pcl::search::KdTree<pcl::PointNormal>::Ptr tree2(
                new pcl::search::KdTree <pcl::PointNormal>);
        tree2->setInputCloud(cloud_with_normals);

        pcl::PolygonMesh triangles;
        // Initialize objects
        pcl::GreedyProjectionTriangulation <pcl::PointNormal> gp3;

        // Set the maximum distance between connected points (maximum edge length)
        gp3.setSearchRadius(0.2);

        // Set typical values for the parameters
        gp3.setMu(3.0);
        gp3.setMaximumNearestNeighbors(100);
        gp3.setMaximumSurfaceAngle(M_PI);
        gp3.setMinimumAngle(M_PI / 10);
        gp3.setMaximumAngle(2 * M_PI / 3.0);
        gp3.setNormalConsistency(false  );
        gp3.setConsistentVertexOrdering(true);

        // Get result
        gp3.setInputCloud(cloud_with_normals);
        gp3.setSearchMethod(tree2);
        gp3.reconstruct(triangles);

        int polygonCount = triangles.polygons.size();
        LOGE("Reconstructed %d polygons", polygonCount);

        array = env->NewFloatArray(polygonCount * 9);
        for (int i = 0; i < polygonCount; i++) {
            float vertex[9] = {
                    cloud_filtered->points[triangles.polygons[i].vertices[0]].x,
                    cloud_filtered->points[triangles.polygons[i].vertices[0]].y,
                    cloud_filtered->points[triangles.polygons[i].vertices[0]].z,
                    cloud_filtered->points[triangles.polygons[i].vertices[1]].x,
                    cloud_filtered->points[triangles.polygons[i].vertices[1]].y,
                    cloud_filtered->points[triangles.polygons[i].vertices[1]].z,
                    cloud_filtered->points[triangles.polygons[i].vertices[2]].x,
                    cloud_filtered->points[triangles.polygons[i].vertices[2]].y,
                    cloud_filtered->points[triangles.polygons[i].vertices[2]].z
            };
            env->SetFloatArrayRegion(array, i * 9, 9, vertex);
        }

        return array;
    }

    void Application::freeArray(JNIEnv * env) {
        if (env->GetArrayLength(array) > 0) {
            env->DeleteGlobalRef(array);
        }
    }

    Application::Application() {
    }

    Application::~Application() {
    }
}