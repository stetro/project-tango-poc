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

#include <tango-gl/conversions.h>
#include "tango-augmented-reality/scene.h"


namespace {
    // We want to represent the device properly with respect to the ground so we'll
    // add an offset in z to our origin. We'll set this offset to 1.3 meters based
    // on the average height of a human standing with a Tango device. This allows us
    // to place a grid roughly on the ground for most users.
    const glm::vec3 kHeightOffset = glm::vec3(0.0f, 0.0f, 0.0f);

    // Color of the motion tracking trajectory.
    const tango_gl::Color kTraceColor(0.22f, 0.28f, 0.67f);

    // Color of the ground grid.
    const tango_gl::Color kGridColor(0.85f, 0.85f, 0.85f);

    // Some property for the AR cube.
    const glm::quat kCubeRotation = glm::quat(0.0f, 0.0f, 1.0f, 0.0f);
    const glm::vec3 kCubePosition = glm::vec3(0.0f, 0.0f, -1.0f);
    const glm::vec3 kCubeScale = glm::vec3(0.05f, 0.05f, 0.05f);
    const tango_gl::Color kCubeColor(1.0f, 0.f, 0.f);

    inline void Yuv2Rgb(uint8_t yValue, uint8_t uValue, uint8_t vValue, uint8_t *r,
                        uint8_t *g, uint8_t *b) {
        *r = yValue + (1.370705 * (vValue - 128));
        *g = yValue - (0.698001 * (vValue - 128)) - (0.337633 * (uValue - 128));
        *b = yValue + (1.732446 * (uValue - 128));

    }
}  // namespace

namespace tango_augmented_reality {

    Scene::Scene() { }

    Scene::~Scene() { }

    void Scene::InitGLContent() {

        depth_width_ = 1280 / 2;
        depth_height_ = 720 / 2;
        gl_depth_format_ = GL_UNSIGNED_SHORT;       // 16 Bit
        cv_depth_format_ = CV_16UC1;                // 16 Bit

        // temporary depth_frame cv buffer
        depth_frame = cv::Mat(depth_height_, depth_width_, cv_depth_format_);

        // create drawable with drawable texture
        depth_drawable_ = new DepthDrawable();

        // create depth texture
        glBindTexture(GL_TEXTURE_2D, depth_drawable_->GetTextureId());
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, depth_width_, depth_height_, 0,
                     GL_DEPTH_COMPONENT, gl_depth_format_, NULL);

        // create frame buffer with color texture and depth
        glGenFramebuffers(1, &depth_frame_buffer_);
        glBindFramebuffer(GL_FRAMEBUFFER, depth_frame_buffer_);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D,
                               depth_drawable_->GetTextureId(), 0);
        GLenum status = glCheckFramebufferStatus(GL_FRAMEBUFFER);

        // check for errors of framebuffer
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            LOGE("ERROR in fb %d ", depth_frame_buffer_);
        }

        // Allocating render camera and drawable object.
        // All of these objects are for visualization purposes.
        yuv_drawable_ = new YUVDrawable();
        gesture_camera_ = new tango_gl::GestureCamera();
        axis_ = new tango_gl::Axis();
        frustum_ = new tango_gl::Frustum();
        trace_ = new tango_gl::Trace();
        grid_ = new tango_gl::Grid();
        cube_ = new tango_gl::Cube();
        point_cloud_drawable_ = new PointCloudDrawable();

        trace_->SetColor(kTraceColor);
        grid_->SetColor(kGridColor);
        grid_->SetPosition(-kHeightOffset);

        cube_->SetPosition(kCubePosition);
        cube_->SetScale(kCubeScale);
        cube_->SetRotation(kCubeRotation);
        cube_->SetColor(kCubeColor);

        int32_t max_point_cloud_elements;
        TangoSupport_createXYZij(20000, &XYZij);

        gesture_camera_->SetCameraType(tango_gl::GestureCamera::CameraType::kThirdPerson);
    }

    void Scene::DeleteResources() {
        delete gesture_camera_;
        delete yuv_drawable_;
        delete depth_drawable_;
        delete axis_;
        delete frustum_;
        delete trace_;
        delete grid_;
        delete cube_;
        delete point_cloud_drawable_;
    }

    void Scene::SetupViewPort(int x, int y, int w, int h) {
        if (h == 0) {
            LOGE("Setup graphic height not valid");
        }
        gesture_camera_->SetAspectRatio(static_cast<float>(w) / static_cast<float>(h));
        glViewport(x, y, w, h);
    }

    void Scene::Render(const glm::mat4 &cur_pose_transformation) {
        if (!is_yuv_texture_available_) {
            return;
        }

        if (depth_fullscreen) {
            depth_drawable_->SetParent(nullptr);
            depth_drawable_->SetScale(glm::vec3(1.0f, 1.0f, 1.0f));
            depth_drawable_->SetPosition(glm::vec3(0.0f, 0.0f, 0.0f));
            depth_drawable_->SetRotation(glm::quat(1.0f, 0.0f, 0.0f, 0.0f));
        } else {
            depth_drawable_->SetParent(nullptr);
            depth_drawable_->SetScale(glm::vec3(0.3f, 0.3f, 0.3f));
            depth_drawable_->SetPosition(glm::vec3(+0.6f, -0.6f, 0.0f));
            depth_drawable_->SetRotation(glm::quat(1.0f, 0.0f, 0.0f, 0.0f));
        }

        ConvertYuvToRGBMat();
        BindRGBMatAsTexture();

        glEnable(GL_DEPTH_TEST);
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

        glm::vec3 position = glm::vec3(cur_pose_transformation[3][0], cur_pose_transformation[3][1],
                                       cur_pose_transformation[3][2]);

        trace_->UpdateVertexArray(position);

        if (gesture_camera_->GetCameraType() == tango_gl::GestureCamera::CameraType::kFirstPerson) {
            // In first person mode, we directly control camera's motion.
            gesture_camera_->SetTransformationMatrix(cur_pose_transformation);
            // If it's first person view, we will render the video overlay in full
            // screen, so we passed identity matrix as view and projection matrix.
            glDisable(GL_DEPTH_TEST);
            yuv_drawable_->Render(glm::mat4(1.0f), glm::mat4(1.0f));
        } else {
            // In third person or top down more, we follow the camera movement.
            gesture_camera_->SetAnchorPosition(position);
            frustum_->SetTransformationMatrix(cur_pose_transformation);
            // Set the frustum scale to 4:3, this doesn't necessarily match the physical
            // camera's aspect ratio, this is just for visualization purposes.
            frustum_->SetScale(glm::vec3(1.0f, camera_image_plane_ratio_, image_plane_distance_));
            frustum_->Render(ar_camera_projection_matrix_, gesture_camera_->GetViewMatrix());
            axis_->SetTransformationMatrix(cur_pose_transformation);
            axis_->Render(ar_camera_projection_matrix_, gesture_camera_->GetViewMatrix());
            trace_->Render(ar_camera_projection_matrix_, gesture_camera_->GetViewMatrix());
            yuv_drawable_->Render(ar_camera_projection_matrix_, gesture_camera_->GetViewMatrix());
        }


        if (show_occlusion) {
            // render reconstructions or pointcloud, depending on mode
            switch (mode) {
                case POINTCLOUD: {
                    std::lock_guard <std::mutex> lock(depth_mutex_);
                    point_cloud_drawable_->Render(gesture_camera_->GetProjectionMatrix(),
                                                  gesture_camera_->GetViewMatrix(),
                                                  point_cloud_transformation, vertices);
                }
                    break;
                case TSDF: {
                    std::lock_guard <std::mutex> lock(depth_mutex_);
                    chisel_mesh_->Render(gesture_camera_->GetProjectionMatrix(),
                                         gesture_camera_->GetViewMatrix());
                }
                    break;
                case PLANE: {
                    std::lock_guard <std::mutex> lock(depth_mutex_);
                    plane_mesh_->Render(gesture_camera_->GetProjectionMatrix(),
                                        gesture_camera_->GetViewMatrix());
                }
                    break;
            }
        }

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);

        // draw depth to framebuffer object
        glBindFramebuffer(GL_FRAMEBUFFER, depth_frame_buffer_);
        glClearColor(0.0, 0.0, 0.0, 0.0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // render reconstructions or pointcloud, depending on mode
        switch (mode) {
            case POINTCLOUD: {
                std::lock_guard <std::mutex> lock(depth_mutex_);
                point_cloud_drawable_->Render(gesture_camera_->GetProjectionMatrix(),
                                              gesture_camera_->GetViewMatrix(),
                                              point_cloud_transformation, vertices);
            }
                break;
            case TSDF: {
                std::lock_guard <std::mutex> lock(depth_mutex_);
                chisel_mesh_->Render(gesture_camera_->GetProjectionMatrix(),
                                     gesture_camera_->GetViewMatrix());
            }
                break;
            case PLANE: {
                std::lock_guard <std::mutex> lock(depth_mutex_);
                plane_mesh_->Render(gesture_camera_->GetProjectionMatrix(),
                                    gesture_camera_->GetViewMatrix());
            }
                break;
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        if (do_filtering) {
            // DEPTH FILTERING ...
            // convert from depth component to mat
            glBindFramebuffer(GL_FRAMEBUFFER, depth_frame_buffer_);
            glReadPixels(0, 0, depth_frame.cols, depth_frame.rows, GL_DEPTH_COMPONENT,
                         gl_depth_format_, depth_frame.ptr());

            // apply opencv filters
            cv::Mat temp_frame(depth_frame.size(), CV_8UC1);
            depth_frame.convertTo(temp_frame, CV_8U, 0.00390625);
            cv::ximgproc::jointBilateralFilter(rgb_frame, temp_frame, temp_frame, 13, 20, 5);
            temp_frame.convertTo(depth_frame, CV_16UC1, 255);

            // copy back to depth texture
            glBindTexture(GL_TEXTURE_2D, depth_drawable_->GetTextureId());
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, depth_frame.cols, depth_frame.rows,
                         0, GL_DEPTH_COMPONENT, gl_depth_format_, depth_frame.ptr());
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }

        // render drawable depth
        depth_drawable_->Render(glm::mat4(1.0f), glm::mat4(1.0f));

        // copy depth to main framebuffer
        glBindFramebuffer(GL_READ_FRAMEBUFFER, depth_frame_buffer_);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBlitFramebuffer(0, 0, depth_width_, depth_height_, 0, 0, depth_width_, depth_height_,
                          GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // render rest of drawables
        grid_->Render(ar_camera_projection_matrix_, gesture_camera_->GetViewMatrix());
        cube_->Render(ar_camera_projection_matrix_, gesture_camera_->GetViewMatrix());

    }

    void Scene::SetCameraType(tango_gl::GestureCamera::CameraType camera_type) {
        gesture_camera_->SetCameraType(camera_type);

        if (camera_type == tango_gl::GestureCamera::CameraType::kFirstPerson) {
            yuv_drawable_->SetParent(nullptr);
            yuv_drawable_->SetScale(glm::vec3(1.0f, 1.0f, 1.0f));
            yuv_drawable_->SetPosition(glm::vec3(0.0f, 0.0f, 0.0f));
            yuv_drawable_->SetRotation(glm::quat(1.0f, 0.0f, 0.0f, 0.0f));
        } else {
            yuv_drawable_->SetScale(glm::vec3(1.0f, camera_image_plane_ratio_, 1.0f));
            yuv_drawable_->SetRotation(glm::quat(1.0f, 0.0f, 0.0f, 0.0f));
            yuv_drawable_->SetPosition(glm::vec3(0.0f, 0.0f, -image_plane_distance_));
            yuv_drawable_->SetParent(axis_);
        }
    }

    void Scene::OnTouchEvent(int touch_count, tango_gl::GestureCamera::TouchEvent event, float x0,
                             float y0, float x1, float y1) {
        gesture_camera_->OnTouchEvent(touch_count, event, x0, y0, x1, y1);
    }

    void Scene::OnFrameAvailable(const TangoImageBuffer *buffer) {
        if (yuv_drawable_->GetTextureId() == 0) {
            LOGE("yuv texture id not valid");
            return;
        }

        if (buffer->format != TANGO_HAL_PIXEL_FORMAT_YCrCb_420_SP) {
            LOGE("yuv texture format is not supported by this app");
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
            rgb_frame = cv::Mat(depth_height_, depth_width_, CV_8UC3);

            glBindTexture(GL_TEXTURE_2D, yuv_drawable_->GetTextureId());
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, rgb_frame.cols, rgb_frame.rows, 0, GL_RGB,
                         GL_UNSIGNED_BYTE, NULL);

            is_yuv_texture_available_ = true;
        }

        std::lock_guard <std::mutex> lock(yuv_buffer_mutex_);
        memcpy(&yuv_temp_buffer_[0], buffer->data, yuv_size_);
        swap_buffer_signal_ = true;
    }

    void Scene::OnXYZijAvailable(const TangoXYZij *XYZ_ij) {
        std::vector <float> points;
        for (int i = 0; i < XYZ_ij->xyz_count; ++i) {
            XYZ_ij->xyz[i][0] = XYZ_ij->xyz[i][0] * .9;
            XYZ_ij->xyz[i][1] = XYZ_ij->xyz[i][1] * 1.2;
            points.push_back(XYZ_ij->xyz[i][0]);
            points.push_back(XYZ_ij->xyz[i][1]);
            points.push_back(XYZ_ij->xyz[i][2]);
        }
        {
            std::lock_guard <std::mutex> lock(depth_mutex_);
            TangoSupport_copyXYZij(XYZ_ij, &XYZij);
            vertices = points;
        }
    }

    void Scene::ConvertYuvToRGBMat() {
        {
            std::lock_guard <std::mutex> lock(yuv_buffer_mutex_);
            if (swap_buffer_signal_) {
                std::swap(yuv_buffer_, yuv_temp_buffer_);
                swap_buffer_signal_ = false;
            }
        }
        size_t x_factor = yuv_height_ / depth_height_;
        size_t y_factor = yuv_width_ / depth_width_;
        for (size_t i = 0; i < yuv_height_; ++i) {
            for (size_t j = 0; j < yuv_width_; ++j) {
                size_t x_index = j;
                if (j % 2 != 0) {
                    x_index = j - 1;
                }
                size_t rgb_index = (i * yuv_width_ + j) * 3;
                if (do_filtering) {
                    cv::Vec3b rgb_dot;
                    Yuv2Rgb(yuv_buffer_[i * yuv_width_ + j],
                            yuv_buffer_[uv_buffer_offset_ + (i / 2) * yuv_width_ + x_index + 1],
                            yuv_buffer_[uv_buffer_offset_ + (i / 2) * yuv_width_ + x_index],
                            &rgb_dot[0], &rgb_dot[1], &rgb_dot[2]);
                    rgb_frame.at<cv::Vec3b>(i / x_factor, j / y_factor) = rgb_dot;
                } else {
                    Yuv2Rgb(yuv_buffer_[i * yuv_width_ + j],
                            yuv_buffer_[uv_buffer_offset_ + (i / 2) * yuv_width_ + x_index + 1],
                            yuv_buffer_[uv_buffer_offset_ + (i / 2) * yuv_width_ + x_index],
                            &rgb_buffer_[rgb_index], &rgb_buffer_[rgb_index + 1],
                            &rgb_buffer_[rgb_index + 2]);
                }
            }
        }
        // flip(rgb_frame, rgb_frame, 0);
    }

    void Scene::BindRGBMatAsTexture() {
        glBindTexture(GL_TEXTURE_2D, yuv_drawable_->GetTextureId());
        if (do_filtering) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, rgb_frame.cols, rgb_frame.rows, 0, GL_RGB,
                         GL_UNSIGNED_BYTE, rgb_frame.ptr());
        } else {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, yuv_width_, yuv_height_, 0, GL_RGB,
                         GL_UNSIGNED_BYTE, rgb_buffer_.data());
        }
    }

    void Scene::ToggleFilter() {
        do_filtering = !do_filtering;
    }

    void Scene::Tap() {
        glm::mat4 transformation = glm::transpose(point_cloud_transformation);
        if (mode == TSDF) {
            LOGD("Collect Points for Chisel");
            {
                std::lock_guard <std::mutex> lock(depth_mutex_);
                chisel_mesh_->addPoints(transformation, depth_intrinsics, &XYZij);
                chisel_mesh_->updateVertices();
            }
        } else if (mode == PLANE) {
            LOGD("Collect Points for Plane Reconstruction");
            {
                std::lock_guard <std::mutex> lock(depth_mutex_);
                plane_mesh_->addPoints(transformation, vertices);
                plane_mesh_->updateVertices();
            }
        }
    }

    void Scene::SetMode(int id) {
        mode = (ARMode) id;
        if (mode == TSDF) {
            chisel_mesh_ = new ChiselMesh();
            chisel_mesh_->init(depth_intrinsics);
        } else if (mode == PLANE) {
            plane_mesh_ = new PlaneMesh();
        }
    }

    void Scene::AddObject(glm::vec3 from, glm::vec3 to) {
        std::lock_guard <std::mutex> lock(depth_mutex_);
        glm::mat4 transformation = glm::transpose(point_cloud_transformation);
        glm::vec4 from_ray = glm::vec4(from, 1) * transformation;
        glm::vec4 to_ray = glm::vec4(to, 1) * transformation;
        for (int i = 0; i < vertices.size() / 3; ++i) {
            glm::vec4 point(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2], 1);
            point = point * transformation;
            glm::vec4 p1;
            glm::vec4 n1;
            glm::vec4 p2;
            glm::vec4 n2;
            if (glm::intersectLineSphere(from_ray, to_ray, point, 0.3, p1, n1, p2, n2)) {
                cube_->SetPosition(glm::vec3(p1));
                break;
            }
        }


    }

}  // namespace tango_augmented_reality
