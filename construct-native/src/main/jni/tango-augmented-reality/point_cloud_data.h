//
// Created by stetro on 06.11.15.
//

#ifndef MASTERPROTOTYPE_POINT_CLOUD_DATA_H
#define MASTERPROTOTYPE_POINT_CLOUD_DATA_H
#include <jni.h>

#include <tango_client_api.h>  // NOLINT
#include <tango-gl/util.h>

namespace tango_augmented_reality {

// PointCloudData is a holder for all point cloud related data. That includes
// the current frame of the point cloud, and some other debug data.
    class PointCloudData {
    public:
        PointCloudData() {}
        ~PointCloudData() {}

        // @return total point count in the current depth frame.
        int GetPointCloudVerticesCount();

        // @return the average depth (in meters) of current depth frame.
        float GetAverageDepth();

        // Set average depth value.
        // @param average_depth: average depth of the current depth frame.
        void SetAverageDepth(float average_depth);

        // @return the delta time (in milliseconds) between the current depth frame
        // and the previous depth frame.
        float GetDepthFrameDeltaTime();

        // Return the current depth frame's timstamp. The timestamp is used for
        // querying the depth frame's pose using the TangoService_getPoseAtTime
        // function, so we return the data in double type.
        //
        // @return current depth frame's timstamp.
        double GetCurrentTimstamp();

        // @return the vector of the vertices.
        const std::vector<float>& GetVerticeVector() { return vertices_; }

        // Update current point cloud data.
        //
        // @param point_cloud: point cloud data of the current frame.
        void UpdatePointCloud(const TangoXYZij* point_cloud);

    private:
        // A vector list of packed coordinate triplets, x,y,z as floating point
        // values With the unit in landscape orientation, screen facing the user:
        // +Z points in the direction of the camera's optical axis, and is measured
        // perpendicular to the plane of the camera.
        // +X points toward the user's right, and +Y points toward the bottom of
        // the screen.
        std::vector<float> vertices_;

        // Timestamp of current depth frame.
        double cur_frame_timstamp_;

        // Previous depth frame's timestamp for computing the delta_timestamp_.
        double prev_frame_timestamp_;

        // delta_timestamp_ is for debug purposes.
        double delta_timestamp_;

        // vertices_count_ is for debug purposes.
        int vertices_count_;

        // average_depth_ is for debug purposes.
        float average_depth_;
    };
}
#endif //MASTERPROTOTYPE_POINT_CLOUD_DATA_H
