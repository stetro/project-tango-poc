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

#ifndef TANGO_AUGMENTED_REALITY_SCENE_H_
#define TANGO_AUGMENTED_REALITY_SCENE_H_

#include <atomic>
#include <jni.h>
#include <memory>
#include <mutex>
#include <string>
#include <sstream>
#include <android/log.h>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "Native",__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG  , "Native",__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO   , "Native",__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN   , "Native",__VA_ARGS__)

#include <tango_client_api.h>  // NOLINT
#include <tango-gl/axis.h>
#include <tango-gl/camera.h>
#include <tango-gl/color.h>
#include <tango-gl/gesture_camera.h>
#include <tango-gl/grid.h>
#include <tango-gl/frustum.h>
#include <tango-gl/cube.h>
#include <tango-gl/trace.h>
#include <tango-gl/transform.h>
#include <tango-gl/util.h>


#include <tango-augmented-reality/pose_data.h>
#include <tango-augmented-reality/point_cloud_drawable.h>
#include <tango-augmented-reality/yuv_drawable.h>
#include <tango-augmented-reality/depth_drawable.h>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/ximgproc.hpp>
#include <opencv2/videostab.hpp>
#include <opencv2/photo.hpp>

namespace tango_augmented_reality {

// Scene provides OpenGL drawable objects and renders them for visualization.
    class Scene {
    public:
        // Constructor and destructor.
        //
        // Scene will need a reference to pose_data_ instance to get the device motion
        // to render the camera frustum.
        Scene();

        ~Scene();

        // Allocate OpenGL resources for rendering.
        void InitGLContent();

        // Release non-OpenGL resources.
        void DeleteResources();

        // Setup GL view port.
        // @param: x, left of the screen.
        // @param: y, bottom of the screen.
        // @param: w, width of the screen.
        // @param: h, height of the screen.
        void SetupViewPort(int x, int y, int w, int h);

        // Render loop.
        void Render(const glm::mat4 &cur_pose_transformation);

        // Set render camera's viewing angle, first person, third person or top down.
        //
        // @param: camera_type, camera type includes first person, third person and
        //         top down
        void SetCameraType(tango_gl::GestureCamera::CameraType camera_type);

        // @return: AR render camera's image plane ratio.
        float GetCameraImagePlaneRatio() { return camera_image_plane_ratio_; }

        // Set AR render camera's image plane ratio.
        // @param: image plane ratio.
        void SetCameraImagePlaneRatio(float ratio) {
            camera_image_plane_ratio_ = ratio;
        }

        // @return: AR render camera's image plane distance from the view point.
        float GetImagePlaneDistance() { return image_plane_distance_; }

        // Set AR render camera's image plane distance from the view point.
        // @param: distance, AR render camera's image plane distance from the view
        //         point.
        void SetImagePlaneDistance(float distance) {
            image_plane_distance_ = distance;
        }

        // Set projection matrix of the AR view (first person view)
        // @param: projection_matrix, the projection matrix.
        void SetARCameraProjectionMatrix(const glm::mat4 &projection_matrix) {
            ar_camera_projection_matrix_ = projection_matrix;
        }

        void SetPointCloudTransformation(glm::mat4 _point_cloud_transformation) {
            point_cloud_transformation = _point_cloud_transformation;
        }

        // Set the frustum render drawable object's scale. For the best visialization
        // result, we set the camera frustum object's scale to the physical camera's
        // aspect ratio.
        // @param: scale, frustum's scale.
        void SetFrustumScale(const glm::vec3 &scale) { frustum_->SetScale(scale); }

        // Clear the Motion Tracking trajactory.
        void ResetTrajectory() { trace_->ClearVertexArray(); }

        // Touch event passed from android activity. This function only support two
        // touches.
        //
        // @param: touch_count, total count for touches.
        // @param: event, touch event of current touch.
        // @param: x0, normalized touch location for touch 0 on x axis.
        // @param: y0, normalized touch location for touch 0 on y axis.
        // @param: x1, normalized touch location for touch 1 on x axis.
        // @param: y1, normalized touch location for touch 1 on y axis.
        void OnTouchEvent(int touch_count, tango_gl::GestureCamera::TouchEvent event,
                          float x0, float y0, float x1, float y1);

        // Updates the yuv_drawable
        void OnFrameAvailable(const TangoImageBuffer *buffer);

        // Updates the depth information
        void OnXYZijAvailable(const TangoXYZij *XYZ_ij);

        void ConvertYuvToRGBMat();

        void BindRGBMatAsTexture();

        void ToggleFilter();

    private:
        // Video overlay drawable object to display the camera image.
        YUVDrawable *yuv_drawable_;

        DepthDrawable *depth_drawable_;

        // Camera object that allows user to use touch input to interact with.
        tango_gl::GestureCamera *gesture_camera_;

        // Device axis (in device frame of reference).
        tango_gl::Axis *axis_;

        // Device frustum.
        tango_gl::Frustum *frustum_;

        // Ground grid.
        tango_gl::Grid *grid_;

        // Trace of pose data.
        tango_gl::Trace *trace_;

        // A cub placed at (0.0f, 0.0f, -1.0f) location.
        tango_gl::Cube *cube_;

        PointCloudDrawable *point_cloud_drawable_;

        // We use both camera_image_plane_ratio_ and image_plane_distance_ to compute
        // the first person AR camera's frustum, these value is derived from actual
        // physical camera instrinsics.
        // Aspect ratio of the color camera.
        float camera_image_plane_ratio_;

        // Image plane distance from camera's origin view point.
        float image_plane_distance_;

        // The projection matrix for the first person AR camera.
        glm::mat4 ar_camera_projection_matrix_;

        glm::mat4 point_cloud_transformation;

        size_t depth_width_;
        size_t depth_height_;

        GLenum gl_depth_format_;
        int cv_depth_format_;


        size_t yuv_width_;
        size_t yuv_height_;
        size_t yuv_size_;
        size_t uv_buffer_offset_;

        std::vector <uint8_t> yuv_buffer_;
        std::vector <uint8_t> yuv_temp_buffer_;
        std::vector <GLubyte> rgb_buffer_;

        std::atomic <bool> is_yuv_texture_available_;
        std::atomic <bool> swap_buffer_signal_;
        std::mutex yuv_buffer_mutex_;

        cv::Mat rgb_frame;
        cv::Mat depth_frame;
        std::vector <float> vertices;

        std::mutex depth_mutex_;

        GLuint depth_frame_buffer_;
        GLuint depth_frame_buffer_depth_texture_;

        bool do_filtering = false;
    };
}  // namespace tango_augmented_reality

#endif  // TANGO_AUGMENTED_REALITY_SCENE_H_
