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

#include "tango-video-overlay/video_overlay_app.h"
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/ximgproc.hpp>
#include <opencv2/videostab.hpp>
#include <opencv2/photo.hpp>
#include <time.h>

cv::Mat depth;
TangoCameraIntrinsics ccIntrinsics;
double lastTime;
// Far clipping plane of the AR camera.
const float kArCameraNearClippingPlane = 0.1f;
const float kArCameraFarClippingPlane = 100.0f;
std::vector <float> vertices_;
int vertices_count_;

enum Filter {
    SMALL = 0, BIG = 1, EDGE = 2, INFILL = 3, COMBINED = 4
};

Filter filter = SMALL;

// from android samples
/* return current time in milliseconds */
static double now_ms(void) {
    struct timespec res;
    clock_gettime(CLOCK_REALTIME, &res);
    return 1000.0 * res.tv_sec + (double) res.tv_nsec / 1e6;
}

double elapsedTime = now_ms();

namespace {
    void OnFrameAvailableRouter(void *context, TangoCameraId, const TangoImageBuffer *buffer) {
        using namespace tango_video_overlay;
        VideoOverlayApp *app = static_cast<VideoOverlayApp *>(context);
        app->OnFrameAvailable(buffer);
    }

    void OnPointCloudAvailableRouter(void *context, const TangoXYZij *xyz_ij) {

        // copy points to avoid concurrency
        size_t point_cloud_size = xyz_ij->xyz_count * 3;
        vertices_.resize(point_cloud_size);
        std::copy(xyz_ij->xyz[0], xyz_ij->xyz[0] + point_cloud_size, vertices_.begin());
        vertices_count_ = xyz_ij->xyz_count;


        //320×180 depth window
        depth = cv::Mat(320, 180, CV_8UC1);
        depth.setTo(cv::Scalar(0, 0, 0));

        // load camera intrinsics
        float fx = static_cast<float>(ccIntrinsics.fx);
        float fy = static_cast<float>(ccIntrinsics.fy);
        float cx = static_cast<float>(ccIntrinsics.cx);
        float cy = static_cast<float>(ccIntrinsics.cy);

        int width = static_cast<int>(ccIntrinsics.width);
        int height = static_cast<int>(ccIntrinsics.height);

        for (int k = 0; k < vertices_count_; ++k) {

            float X = vertices_[k * 3];
            float Y = vertices_[k * 3 + 1];
            float Z = vertices_[k * 3 + 2];

            // project points with intrinsics
            int x = static_cast<int>(fx * (X / Z) + cx);
            int y = static_cast<int>(fy * (Y / Z) + cy);

            if (x < 0 || x > width || y < 0 || y > height) {
                continue;
            }

            uint8_t depth_value = UCHAR_MAX - ((Z * 1000) * UCHAR_MAX / 4500);

            cv::Point point(y, x);
            if (filter == SMALL) {
                line(depth, point, point, cv::Scalar(depth_value), 1.0);
            } else {
                line(depth, point, point, cv::Scalar(depth_value), 5.0);
            }

        }


    }

    // We could do this conversion in a fragment shader if all we care about is
    // rendering, but we show it here as an example of how people can use RGB data
    // on the CPU.
    inline void Yuv2Rgb(uint8_t yValue, uint8_t uValue, uint8_t vValue, uint8_t *r,
                        uint8_t *g, uint8_t *b) {
        *r = yValue + (1.370705 * (vValue - 128));
        *g = yValue - (0.698001 * (vValue - 128)) - (0.337633 * (uValue - 128));
        *b = yValue + (1.732446 * (uValue - 128));
    }

}

namespace tango_video_overlay {

    VideoOverlayApp::VideoOverlayApp() {
        is_yuv_texture_available_ = false;
        swap_buffer_signal_ = false;
        video_overlay_drawable_ = NULL;
        yuv_drawable_ = NULL;
    }

    VideoOverlayApp::~VideoOverlayApp() {
        if (tango_config_ != nullptr) {
            TangoConfig_free(tango_config_);
        }
    }

    void VideoOverlayApp::OnFrameAvailable(const TangoImageBuffer *buffer) {
        if (current_texture_method_ != TextureMethod::kYUV) {
            return;
        }

        if (yuv_drawable_->GetTextureId() == 0) {
            LOGE("VideoOverlayApp::yuv texture id not valid");
            return;
        }

        if (buffer->format != TANGO_HAL_PIXEL_FORMAT_YCrCb_420_SP) {
            LOGE("VideoOverlayApp::yuv texture format is not supported by this app");
            return;
        }

        // The memory needs to be allocated after we get the first frame because we
        // need to know the size of the image.
        if (!is_yuv_texture_available_) {
            yuv_width_ = buffer->width;
            yuv_height_ = buffer->height;
            uv_buffer_offset_ = yuv_width_ * yuv_height_;

            yuv_size_ = yuv_width_ * yuv_height_ + yuv_width_ * yuv_height_ / 2;

            // Reserve and resize the buffer size for RGB and YUV data.
            yuv_buffer_.resize(yuv_size_);
            yuv_temp_buffer_.resize(yuv_size_);
            rgb_buffer_.resize(yuv_width_ * yuv_height_ * 3);

            AllocateTexture(yuv_drawable_->GetTextureId(), yuv_width_, yuv_height_);
            is_yuv_texture_available_ = true;
        }

        std::lock_guard <std::mutex> lock(yuv_buffer_mutex_);
        memcpy(&yuv_temp_buffer_[0], buffer->data, yuv_size_);
        swap_buffer_signal_ = true;
    }

    int VideoOverlayApp::TangoInitialize(JNIEnv *env, jobject caller_activity) {
        // The first thing we need to do for any Tango enabled application is to
        // initialize the service. We'll do that here, passing on the JNI environment
        // and jobject corresponding to the Android activity that is calling us.
        return TangoService_initialize(env, caller_activity);
    }

    int VideoOverlayApp::TangoSetupConfig() {
        // Here, we'll configure the service to run in the way we'd want. For this
        // application, we'll start from the default configuration
        // (TANGO_CONFIG_DEFAULT). This enables basic motion tracking capabilities.
        tango_config_ = TangoService_getConfig(TANGO_CONFIG_DEFAULT);
        if (tango_config_ == nullptr) {
            LOGE("VideoOverlayApp: Failed to get default config form");
            return TANGO_ERROR;
        }

        // Enable color camera from config.
        int ret = TangoConfig_setBool(tango_config_, "config_enable_color_camera", true);
        if (ret != TANGO_SUCCESS) {
            LOGE("VideoOverlayApp: config_enable_color_camera() failed - error code: %d", ret);
            return ret;
        }
        // Enable depth camera from config.
        ret = TangoConfig_setBool(tango_config_, "config_enable_depth", true);
        if (ret != TANGO_SUCCESS) {
            LOGE("VideoOverlayApp: config_enable_depth() failed - error code: %d", ret);
            return ret;
        }
        // Enable depth sensing
        ret = TangoService_connectOnXYZijAvailable(OnPointCloudAvailableRouter);
        if (ret != TANGO_SUCCESS) {
            LOGE("VideoOverlayApp: Failed to connect to point cloud callback - error code: %d",
                 ret);
            return ret;
        }

        // Get camrea intrinsics
        ret = TangoService_getCameraIntrinsics(TANGO_CAMERA_DEPTH, &ccIntrinsics);
        if (ret != TANGO_SUCCESS) {
            LOGE("VideoOverlayApp: Failed get camrea intrinsics - error code: %d", ret);
            return ret;
        }

        if (ccIntrinsics.calibration_type == TANGO_CALIBRATION_POLYNOMIAL_3_PARAMETERS) {
            LOGE("TANGO_CALIBRATION_POLYNOMIAL_3_PARAMETERS");
        } else {
            LOGE("TANGO_CALIBRATION_EQUIDISTANT %d", ccIntrinsics.calibration_type);
        }

        ret = TangoService_connectOnFrameAvailable(TANGO_CAMERA_COLOR, this,
                                                   OnFrameAvailableRouter);
        if (ret != TANGO_SUCCESS) {
            LOGE("VideoOverlayApp: Error connecting color frame %d", ret);
        }
        return ret;
    }

    // Connect to Tango Service, service will start running, and
    // pose can be queried.
    int VideoOverlayApp::TangoConnect() {
        TangoErrorType ret = TangoService_connect(this, tango_config_);
        if (ret != TANGO_SUCCESS) {
            LOGE("VideoOverlayApp: Failed to connect to the Tango service - error code: %d", ret);
            return ret;
        }
        return ret;
    }

    void VideoOverlayApp::TangoDisconnect() {
        // When disconnecting from the Tango Service, it is important to make sure to
        // free your configuration object. Note that disconnecting from the service,
        // resets all configuration, and disconnects all callbacks. If an application
        // resumes after disconnecting, it must re-register configuration and
        // callbacks with the service.
        TangoConfig_free(tango_config_);
        tango_config_ = nullptr;
        TangoService_disconnect();
    }

    void VideoOverlayApp::DeleteDrawables() {
        delete video_overlay_drawable_;
        delete yuv_drawable_;
        video_overlay_drawable_ = NULL;
        yuv_drawable_ = NULL;
    }

    void VideoOverlayApp::InitializeGLContent() {
        if (video_overlay_drawable_ != NULL || yuv_drawable_ != NULL) {
            this->DeleteDrawables();
        }
        video_overlay_drawable_ = new tango_gl::VideoOverlay();
        yuv_drawable_ = new YUVDrawable();

        // Connect color camera texture. TangoService_connectTextureId expects a valid
        // texture id from the caller, so we will need to wait until the GL content is
        // properly allocated.
        int texture_id = static_cast<int>(video_overlay_drawable_->GetTextureId());
        TangoErrorType ret = TangoService_connectTextureId(TANGO_CAMERA_COLOR, texture_id, nullptr,
                                                           nullptr);
        if (ret != TANGO_SUCCESS) {
            LOGE("VideoOverlayApp: Failed to connect the texture id with error code: %d", ret);
        }
    }

    void VideoOverlayApp::SetViewPort(int width, int height) {
        glViewport(0, 0, width, height);
    }

    void VideoOverlayApp::Render() {
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        switch (current_texture_method_) {
            case TextureMethod::kYUV:
                RenderYUV();
                break;
            case TextureMethod::kTextureId:
                RenderTextureId();
                break;
        }
    }

    void VideoOverlayApp::FreeBufferData() {
        is_yuv_texture_available_ = false;
        swap_buffer_signal_ = false;
        rgb_buffer_.clear();
        yuv_buffer_.clear();
        yuv_temp_buffer_.clear();
        this->DeleteDrawables();
    }

    void VideoOverlayApp::AllocateTexture(GLuint texture_id, int width,
                                          int height) {

        glBindTexture(GL_TEXTURE_2D, texture_id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB,
                     GL_UNSIGNED_BYTE, rgb_buffer_.data());
    }

    void VideoOverlayApp::RenderYUV() {
        if (!is_yuv_texture_available_) {
            return;
        }
        {
            std::lock_guard <std::mutex> lock(yuv_buffer_mutex_);
            if (swap_buffer_signal_) {
                std::swap(yuv_buffer_, yuv_temp_buffer_);
                swap_buffer_signal_ = false;
            }
        }

        cv::Mat rgb(yuv_width_, yuv_height_, CV_8UC3);

        for (size_t i = 0; i < yuv_height_; ++i) {
            for (size_t j = 0; j < yuv_width_; ++j) {
                size_t x_index = j;
                if (j % 2 != 0) {
                    x_index = j - 1;
                }

                size_t rgb_index = (i * yuv_width_ + j) * 3;

                Yuv2Rgb(yuv_buffer_[i * yuv_width_ + j],
                        yuv_buffer_[uv_buffer_offset_ + (i / 2) * yuv_width_ + x_index + 1],
                        yuv_buffer_[uv_buffer_offset_ + (i / 2) * yuv_width_ + x_index],
                        &rgb_buffer_[rgb_index], &rgb_buffer_[rgb_index + 1],
                        &rgb_buffer_[rgb_index + 2]);

                rgb.at<cv::Vec3b>(j, i)[0] = rgb_buffer_[rgb_index];
                rgb.at<cv::Vec3b>(j, i)[1] = rgb_buffer_[rgb_index + 1];
                rgb.at<cv::Vec3b>(j, i)[2] = rgb_buffer_[rgb_index + 2];
            }
        }

        if (!depth.empty()) {
            cv::Mat tmp_depth(depth);



            filter = (Filter) (((int) ((now_ms() - elapsedTime) / 5000)) % 5);

            cv::Mat scaled_rgb(320, 180, CV_8UC3);
            if (filter == INFILL || filter == COMBINED) {
                inpaint(tmp_depth, (tmp_depth == 0), tmp_depth, 3.0, 1);
            }
            resize(rgb, scaled_rgb, scaled_rgb.size());
            if (filter == EDGE || filter == COMBINED) {
                cv::ximgproc::guidedFilter(scaled_rgb, tmp_depth, tmp_depth, 5, 2.0);
            }
            cv::cvtColor(tmp_depth, tmp_depth, CV_GRAY2RGB);
            addWeighted(scaled_rgb, 0.0, tmp_depth, 1.0, 0.0, scaled_rgb);

            resize(scaled_rgb, scaled_rgb,cv::Size(360,640));

            int space = 20;
            int x2 = yuv_height_ - space;
            int x1 = yuv_height_ - (scaled_rgb.cols + space);
            int y2 = yuv_width_ - space;
            int y1 = yuv_width_ - (scaled_rgb.rows + space);

            scaled_rgb.copyTo(rgb.rowRange(y1, y2).colRange(x1, x2));

            double currentTime = now_ms();
            LOGE("%lf Hz", 1000 / (currentTime - lastTime));
            lastTime = currentTime;

        }

        for (size_t i = 0; i < yuv_height_; ++i) {
            for (size_t j = 0; j < yuv_width_; ++j) {
                size_t rgb_index = (i * yuv_width_ + j) * 3;
                rgb_buffer_[rgb_index + 0] = rgb.at<cv::Vec3b>(j, i)[0];
                rgb_buffer_[rgb_index + 1] = rgb.at<cv::Vec3b>(j, i)[1];
                rgb_buffer_[rgb_index + 2] = rgb.at<cv::Vec3b>(j, i)[2];
            }
        }

        glBindTexture(GL_TEXTURE_2D, yuv_drawable_->GetTextureId());
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, yuv_width_, yuv_height_, 0, GL_RGB,
                     GL_UNSIGNED_BYTE, rgb_buffer_.data());

        yuv_drawable_->Render(glm::mat4(1.0f), glm::mat4(1.0f));
    }

    void VideoOverlayApp::RenderTextureId() {
        double timestamp;
        // TangoService_updateTexture() updates target camera's
        // texture and timestamp.
        int ret = TangoService_updateTexture(TANGO_CAMERA_COLOR, &timestamp);
        if (ret != TANGO_SUCCESS) {
            LOGE("VideoOverlayApp: Failed to update the texture id with error code: %d", ret);
        }
        video_overlay_drawable_->Render(glm::mat4(1.0f), glm::mat4(1.0f));
    }

}  // namespace tango_video_overlay
