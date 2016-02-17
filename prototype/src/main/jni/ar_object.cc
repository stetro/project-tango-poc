#include "tango-augmented-reality/ar_object.h"


namespace tango_augmented_reality {

    static const GLfloat const_vertices[] = {
            -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
            1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
            1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f,
            -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f};

    static const GLfloat const_normals[] = {
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, -1.0f,
            0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f};

    ArObject::ArObject() {
        SetShader(true);

        std::ifstream input("/storage/emulated/0/teapot.ply");
        std::string line;

        if (input.is_open()) {
            std::vector <glm::vec3> vertices;
            std::vector <GLfloat> final_vertices;
            std::vector <GLfloat> final_normals;

            int vertices_count = 0;
            int indices_count = 0;

            bool end_of_header = false;
            do {
                std::getline(input, line);
                line = line.substr(0, line.size() - 1);
                std::sscanf(line.c_str(), "element vertex %d", &vertices_count);
                std::sscanf(line.c_str(), "element face %d", &indices_count);
                if (line.compare("end_header") == 0) {
                    end_of_header = true;
                }
            } while (!end_of_header);

            for (int i = 0; i < vertices_count; ++i) {
                std::getline(input, line);
                line = line.substr(0, line.size() - 1);
                float x, y, z;
                std::sscanf(line.c_str(), "%f %f %f", &x, &y, &z);
                vertices.push_back(glm::vec3(x, y, z));
            }

            for (int i = 0; i < indices_count; ++i) {
                std::getline(input, line);
                line = line.substr(0, line.size() - 1);
                unsigned short p0, p1, p2;
                std::sscanf(line.c_str(), "3 %hu %hu %hu", &p0, &p1, &p2);

                glm::vec3 normal = glm::cross(vertices[p1]-vertices[p0],vertices[p2]-vertices[p0]);
                normal = glm::normalize(normal);
                for (int j = 0; j < 3; ++j) {
                    final_normals.push_back(normal.x);
                    final_normals.push_back(normal.y);
                    final_normals.push_back(normal.z);
                }
                final_vertices.push_back(vertices[p0].x);
                final_vertices.push_back(vertices[p0].y);
                final_vertices.push_back(vertices[p0].z);

                final_vertices.push_back(vertices[p1].x);
                final_vertices.push_back(vertices[p1].y);
                final_vertices.push_back(vertices[p1].z);

                final_vertices.push_back(vertices[p2].x);
                final_vertices.push_back(vertices[p2].y);
                final_vertices.push_back(vertices[p2].z);
            }

            SetVertices(final_vertices, final_normals);
        } else {
            std::vector <GLfloat> vertices(const_vertices,
                                           const_vertices +
                                           sizeof(const_vertices) /
                                           sizeof(GLfloat));
            std::vector <GLfloat> normals(const_normals,
                                          const_normals +
                                          sizeof(const_normals) /
                                          sizeof(GLfloat));
            SetVertices(vertices, normals);
        }
    }
}  // namespace tango_gl