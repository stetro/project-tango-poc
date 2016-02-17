#ifndef AR_OBJECT_H
#define AR_OBJECT_H

#include <tango-gl/mesh.h>

#include <string>
#include <iostream>
#include <fstream>

namespace tango_augmented_reality {
    class ArObject : public tango_gl::Mesh {
    public:
        ArObject();
    };
}
#endif  // AR_OBJECT_H