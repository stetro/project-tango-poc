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

#include "tango-plane-fitting/plane_fitting_application.h"
#include "tango-plane-fitting/plane_fitting.h"

#include <vector>

#include <glm/gtc/matrix_access.hpp>
#include <glm/gtx/quaternion.hpp>
#include <tango-gl/camera.h>
#include <tango-gl/conversions.h>
#include <tango-gl/util.h>
#include <tango-gl/mesh.h>

#include <pcl/point_types.h>
#include <pcl/kdtree/kdtree_flann.h>
#include <pcl/kdtree/impl/kdtree_flann.hpp>
#include <pcl/features/normal_3d.h>
#include <pcl/surface/gp3.h>
#include <pcl/surface/marching_cubes_rbf.h>

volatile bool newData = false;

std::vector <GLfloat> vertices_;
std::vector <GLushort> indices_;

double timestamp = 0.0;

namespace tango_plane_fitting {

    namespace {

        constexpr float kCubeScale = 0.05f;

        /**
         * This function will route callbacks to our application object via the context
         * parameter.
         *
         * @param context Will be a pointer to a PlaneFittingApplication instance on
         * which to call callbacks.
         * @param xyz_ij The point cloud to pass on.
         */
        void OnXYZijAvailableRouter(void *context, const TangoXYZij *xyz_ij) {
            PlaneFittingApplication *app = static_cast<PlaneFittingApplication *>(context);
            timestamp = xyz_ij->timestamp;
            app->OnXYZijAvailable(xyz_ij);
        }

    }  // end namespace

    void PlaneFittingApplication::OnXYZijAvailable(const TangoXYZij *xyz_ij) {
        point_cloud_->UpdateVertices(xyz_ij);
    }

    PlaneFittingApplication::PlaneFittingApplication()
            : point_cloud_debug_render_(false),
              opengl_world_T_start_service_(
                      tango_gl::conversions::opengl_world_T_tango_world()),
              color_camera_T_opengl_camera_(
                      tango_gl::conversions::color_camera_T_opengl_camera()) { }

    PlaneFittingApplication::~PlaneFittingApplication() {
        TangoConfig_free(tango_config_);
    }

    int PlaneFittingApplication::TangoInitialize(JNIEnv *env, jobject caller_activity) {
        // The first thing we need to do for any Tango enabled application is to
        // initialize the service. We will do that here, passing on the JNI
        // environment and jobject corresponding to the Android activity that is
        // calling us.
        const int ret = TangoService_initialize(env, caller_activity);
        if (ret != TANGO_SUCCESS) {
            return ret;
        }

        tango_config_ = TangoService_getConfig(TANGO_CONFIG_DEFAULT);
        if (tango_config_ == nullptr) {
            LOGE("Unable to get tango config");
            return TANGO_ERROR;
        }
        return TANGO_SUCCESS;
    }

    int PlaneFittingApplication::TangoSetupAndConnect() {
        // Here, we will configure the service to run in the way we would want. For
        // this application, we will start from the default configuration
        // (TANGO_CONFIG_DEFAULT). This enables basic motion tracking capabilities.
        // In addition to motion tracking, however, we want to run with depth so that
        // we can measure things. As such, we are going to set an additional flag
        // "config_enable_depth" to true.
        if (tango_config_ == nullptr) {
            return TANGO_ERROR;
        }

        TangoErrorType ret = TangoConfig_setBool(tango_config_, "config_enable_depth", true);
        if (ret != TANGO_SUCCESS) {
            LOGE("Failed to enable depth.");
            return ret;
        }

        // Enable scene reconstruction
        ret = TangoConfig_setBool(tango_config_, "config_experimental_enable_scene_reconstruction",
                                  true);
        if (ret != TANGO_SUCCESS) {
            LOGE("config_experimental_enable_scene_reconstruction() failed with error code: %d",
                 ret);
            return ret;
        }

        ret = TangoConfig_setBool(tango_config_, "config_enable_color_camera", true);
        if (ret != TANGO_SUCCESS) {
            LOGE("Failed to enable color camera.");
            return ret;
        }

        // Note that it is super important for AR applications that we enable low
        // latency IMU integration so that we have pose information available as
        // quickly as possible. Without setting this flag, you will often receive
        // invalid poses when calling getPoseAtTime() for an image.
        ret = TangoConfig_setBool(tango_config_, "config_enable_low_latency_imu_integration", true);
        if (ret != TANGO_SUCCESS) {
            LOGE("Failed to enable low latency imu integration.");
            return ret;
        }

        // Register for depth notification.
        ret = TangoService_connectOnXYZijAvailable(OnXYZijAvailableRouter);
        if (ret != TANGO_SUCCESS) {
            LOGE("Failed to connected to depth callback.");
            return ret;
        }

        // Here, we will connect to the TangoService and set up to run. Note that
        // we are passing in a pointer to ourselves as the context which will be
        // passed back in our callbacks.
        ret = TangoService_connect(this, tango_config_);
        if (ret != TANGO_SUCCESS) {
            LOGE("PlaneFittingApplication: Failed to connect to the Tango service.");
            return ret;
        }

        // Get the intrinsics for the color camera and pass them on to the depth
        // image. We need these to know how to project the point cloud into the color
        // camera frame.
        ret = TangoService_getCameraIntrinsics(TANGO_CAMERA_COLOR, &color_camera_intrinsics_);
        if (ret != TANGO_SUCCESS) {
            LOGE("PlaneFittingApplication: Failed to get the intrinsics for the color camera.");
        }

        constexpr float kNearPlane = 0.1;
        constexpr float kFarPlane = 100.0;

        projection_matrix_ar_ = tango_gl::Camera::ProjectionMatrixForCameraIntrinsics(
                color_camera_intrinsics_.width, color_camera_intrinsics_.height,
                color_camera_intrinsics_.fx, color_camera_intrinsics_.fy,
                color_camera_intrinsics_.cx, color_camera_intrinsics_.cy, kNearPlane,
                kFarPlane);

        // Setup fixed pose information
        TangoPoseData pose_imu_T_color_t0;
        TangoPoseData pose_imu_T_depth_t0;
        TangoPoseData pose_imu_T_device_t0;

        TangoCoordinateFramePair frame_pair;
        glm::vec3 translation;
        glm::quat rotation;

        // We need to get the extrinsic transform between the color camera and the
        // IMU coordinate frames. This matrix is then used to compute the extrinsic
        // transform between color camera and device: C_T_D = C_T_IMU * IMU_T_D.
        // Note that the matrix C_T_D is a constant transformation since the hardware
        // will not change, we use the getPoseAtTime() function to query it once right
        // after the Tango Service connected and store it for efficiency.
        frame_pair.base = TANGO_COORDINATE_FRAME_IMU;
        frame_pair.target = TANGO_COORDINATE_FRAME_DEVICE;
        ret = TangoService_getPoseAtTime(0.0, frame_pair, &pose_imu_T_device_t0);
        if (ret != TANGO_SUCCESS) {
            LOGE("PlaneFittingApplication: Failed to get transform between the IMU and device frames. Something is wrong with device extrinsics.");
            return ret;
        }
        const glm::mat4 IMU_T_device = tango_gl::conversions::TransformFromArrays(
                pose_imu_T_device_t0.translation, pose_imu_T_device_t0.orientation);

        // Get color camera with respect to IMU transformation matrix. This matrix is
        // used to compute the extrinsics between color camera and device:
        // C_T_D = C_T_IMU * IMU_T_D.
        frame_pair.base = TANGO_COORDINATE_FRAME_IMU;
        frame_pair.target = TANGO_COORDINATE_FRAME_CAMERA_COLOR;
        ret = TangoService_getPoseAtTime(0.0, frame_pair, &pose_imu_T_color_t0);
        if (ret != TANGO_SUCCESS) {
            LOGE("PlaneFittingApplication: Failed to get transform between the IMU and camera frames. Something is wrong with device extrinsics.");
            return ret;
        }
        const glm::mat4 IMU_T_color = tango_gl::conversions::TransformFromArrays(
                pose_imu_T_color_t0.translation, pose_imu_T_color_t0.orientation);

        // Get depth camera with respect to imu transformation matrix. This matrix is
        // used to compute the extrinsics between depth camera and device:
        // C_T_D = C_T_IMU * IMU_T_D.
        frame_pair.base = TANGO_COORDINATE_FRAME_IMU;
        frame_pair.target = TANGO_COORDINATE_FRAME_CAMERA_DEPTH;
        ret = TangoService_getPoseAtTime(0.0, frame_pair, &pose_imu_T_depth_t0);
        if (ret != TANGO_SUCCESS) {
            LOGE("PlaneFittingApplication: Failed to get transform between the IMU and camera frames. Something is wrong with device extrinsics.");
            return ret;
        }
        const glm::mat4 IMU_T_depth = tango_gl::conversions::TransformFromArrays(
                pose_imu_T_depth_t0.translation, pose_imu_T_depth_t0.orientation);

        device_T_depth_ = glm::inverse(IMU_T_device) * IMU_T_depth;
        device_T_color_ = glm::inverse(IMU_T_device) * IMU_T_color;

//        if (TangoService_Experimental_startSceneReconstruction() == TANGO_SUCCESS) {
//            LOGE("Reconstruction started ...");
//            if (TangoService_Experimental_connectOnMeshVectorAvailable(ExtractMesh) ==
//                TANGO_SUCCESS) {
//                LOGE("Registered Callback ...");
//            }
//        } else {
//            LOGE("Could not start Reconstruction");
//            return ret;
//        }

        return ret;
    }

    void PlaneFittingApplication::TangoDisconnect() {
        TangoService_disconnect();
    }

    int PlaneFittingApplication::InitializeGLContent() {
        int32_t max_point_cloud_elements;
        const int ret = TangoConfig_getInt32(tango_config_, "max_point_cloud_elements",
                                             &max_point_cloud_elements);
        if (ret != TANGO_SUCCESS) {
            LOGE("Failed to query maximum number of point cloud elements.");
            return ret;
        }

        video_overlay_ = new tango_gl::VideoOverlay();

        point_cloud_ = new PointCloud(max_point_cloud_elements);

        cube_ = new tango_gl::Cube();
        cube_->SetScale(glm::vec3(kCubeScale, kCubeScale, kCubeScale));
        cube_->SetColor(0.2f, 0.8f, 0.2f);

        mesh_ = new tango_gl::Mesh();
        mesh_->SetColor(0.2f, 0.8f, 0.3f);
        mesh_->SetShader(false);


        // The Tango service allows you to connect an OpenGL texture directly to its
        // RGB and fisheye cameras. This is the most efficient way of receiving
        // images from the service because it avoids copies. You get access to the
        // graphic buffer directly. As we are interested in rendering the color image
        // in our render loop, we will be polling for the color image as needed.
        return TangoService_connectTextureId(TANGO_CAMERA_COLOR, video_overlay_->GetTextureId(),
                                             this, nullptr);
    }

    void PlaneFittingApplication::SetRenderDebugPointCloud(bool on) {
        point_cloud_->SetRenderDebugColors(on);
    }

    void PlaneFittingApplication::SetViewPort(int width, int height) {
        screen_width_ = static_cast<float>(width);
        screen_height_ = static_cast<float>(height);

        glViewport(0, 0, screen_width_, screen_height_);
    }

    void PlaneFittingApplication::Render() {
        double color_gpu_timestamp = 0.0;
        // We need to make sure that we update the texture associated with the color
        // image.
        if (TangoService_updateTexture(TANGO_CAMERA_COLOR, &color_gpu_timestamp) !=
            TANGO_SUCCESS) {
            LOGE("PlaneFittingApplication: Failed to get a color image.");
            return;
        }

        // Querying the GPU color image's frame transformation based its timestamp.
        TangoPoseData pose_start_service_T_color_gpu;
        TangoCoordinateFramePair color_gpu_frame_pair;
        color_gpu_frame_pair.base = TANGO_COORDINATE_FRAME_START_OF_SERVICE;
        color_gpu_frame_pair.target = TANGO_COORDINATE_FRAME_DEVICE;
        if (TangoService_getPoseAtTime(color_gpu_timestamp, color_gpu_frame_pair,
                                       &pose_start_service_T_color_gpu) !=
            TANGO_SUCCESS) {
            LOGE(
                    "PlaneFittingApplication: Could not find a valid pose at time %lf"
                            " for the color camera.",
                    color_gpu_timestamp);
        }

        if (pose_start_service_T_color_gpu.status_code == TANGO_POSE_VALID) {
            const glm::mat4 start_service_T_device =
                    tango_gl::conversions::TransformFromArrays(
                            pose_start_service_T_color_gpu.translation,
                            pose_start_service_T_color_gpu.orientation);

            const glm::mat4 start_service_T_color_camera =
                    start_service_T_device * device_T_color_;

            GLRender(start_service_T_color_camera);
        } else {
            LOGE("Invalid pose for gpu color image at time: %lf", color_gpu_timestamp);
        }

    }

    void PlaneFittingApplication::GLRender(const glm::mat4 &start_service_T_color_camera) {
        glEnable(GL_CULL_FACE);

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // We want to render from the perspective of the device, so we will set our
        // camera based on the transform that was passed in.
        glm::mat4 opengl_camera_T_ss = glm::inverse(
                start_service_T_color_camera * color_camera_T_opengl_camera_);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        video_overlay_->Render(glm::mat4(1.0), glm::mat4(1.0));
        glEnable(GL_DEPTH_TEST);
        point_cloud_->Render(projection_matrix_ar_, opengl_camera_T_ss, device_T_depth_);
        glDisable(GL_BLEND);

        glm::mat4 opengl_camera_T_opengl_world = opengl_camera_T_ss * glm::inverse(
                tango_gl::conversions::opengl_world_T_tango_world());
        cube_->Render(projection_matrix_ar_, opengl_camera_T_opengl_world);

        if (newData) {
            mesh_->SetVertices(vertices_, indices_);
            newData = false;
        } else {
            mesh_->Render(projection_matrix_ar_, opengl_camera_T_opengl_world);
        }

    }

    void PlaneFittingApplication::FreeGLContent() {
        delete video_overlay_;
        delete point_cloud_;
        delete cube_;
        delete mesh_;
        video_overlay_ = nullptr;
        point_cloud_ = nullptr;
        cube_ = nullptr;
        mesh_ = nullptr;
    }

// We assume the Java layer ensures this function is called on the GL thread.
    void PlaneFittingApplication::OnTouchEvent(float x, float y) {

        const glm::mat4 depth_camera_T_opengl_camera = tango_gl::conversions::depth_camera_T_opengl_camera();

        // Get the current point cloud data and transform.  This assumes the data has
        // been recently updated on the render thread and does not attempt to update
        // again here.
        const TangoXYZij *current_cloud = point_cloud_->GetCurrentPointData();
        pcl::PointCloud<pcl::PointXYZ>::Ptr cloud(new pcl::PointCloud <pcl::PointXYZ>);
        cloud->points.resize(current_cloud->xyz_count);
        for (int i = 0; i < current_cloud->xyz_count; ++i) {
            glm::vec4 originalPoint = glm::vec4(
                    glm::vec3(current_cloud->xyz[i][0],
                              current_cloud->xyz[i][1],
                              current_cloud->xyz[i][2]), 1.0);
            glm::vec4 transformedPoint = (originalPoint * depth_camera_T_opengl_camera);

            pcl::PointXYZ point;
            point.x = transformedPoint[0];
            point.y = transformedPoint[1];
            point.z = transformedPoint[2];
            cloud->points[i] = point;
        }

        LOGE("PointCloud has %d points", cloud->points.size());

        // Normal estimation*
        pcl::NormalEstimation <pcl::PointXYZ, pcl::Normal> n;
        pcl::PointCloud<pcl::Normal>::Ptr normals(new pcl::PointCloud <pcl::Normal>);
        pcl::search::KdTree<pcl::PointXYZ>::Ptr tree(new pcl::search::KdTree <pcl::PointXYZ>);
        tree->setInputCloud(cloud);
        n.setInputCloud(cloud);
        n.setSearchMethod(tree);
        n.setKSearch(20);
        n.compute(*normals);
        //* normals should not contain the point normals + surface curvatures

        // Concatenate the XYZ and normal fields*
        pcl::PointCloud<pcl::PointNormal>::Ptr cloud_with_normals(
                new pcl::PointCloud <pcl::PointNormal>);
        pcl::concatenateFields(*cloud, *normals, *cloud_with_normals);
        //* cloud_with_normals = cloud + normals

        // Create search tree*
        pcl::search::KdTree<pcl::PointNormal>::Ptr tree2(
                new pcl::search::KdTree <pcl::PointNormal>);
        tree2->setInputCloud(cloud_with_normals);

        pcl::PolygonMesh triangles;
        // Initialize objects
        pcl::GreedyProjectionTriangulation <pcl::PointNormal> gp3;

        // Set the maximum distance between connected points (maximum edge length)
        gp3.setSearchRadius(0.4);

        // Set typical values for the parameters
        gp3.setMu(2.5);
        gp3.setMaximumNearestNeighbors (100);
        gp3.setMaximumSurfaceAngle(M_PI);
        gp3.setMinimumAngle(M_PI/4);
        gp3.setMaximumAngle(M_PI/2.0);
        gp3.setNormalConsistency(false);
        gp3.setConsistentVertexOrdering(true);

        // Get result
        gp3.setInputCloud(cloud_with_normals);
        gp3.setSearchMethod(tree2);
        gp3.reconstruct(triangles);


        LOGE("Reconstructed %d polygons", triangles.polygons.size());


        vertices_.clear();
        indices_.clear();

        for (int i = 0; i < cloud->points.size(); i++) {
            vertices_.push_back(cloud->points[i].x);
            vertices_.push_back(cloud->points[i].y);
            vertices_.push_back(cloud->points[i].z);
        }
        LOGE("converting to vectors - vertices DONE");

        for (int i = 0; i < triangles.polygons.size(); i++) {
            indices_.push_back(triangles.polygons[i].vertices[0]);
            indices_.push_back(triangles.polygons[i].vertices[1]);
            indices_.push_back(triangles.polygons[i].vertices[2]);
            if(triangles.polygons[i].vertices.size()>3){
                LOGE("Matrix is Broken!!");
            }
        }
        LOGE("converting to vectors - indices DONE");
        newData = true;
    }

}  // namespace tango_plane_fitting
