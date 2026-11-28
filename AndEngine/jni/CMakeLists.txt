# osu!droid ONLY
cmake_minimum_required(VERSION 3.18.1)

project(osudroid_frame_timestamps CXX)

add_library(osudroid_frame_timestamps SHARED frame_timestamps.cpp)

set_target_properties(osudroid_frame_timestamps PROPERTIES
        CXX_STANDARD 17
        CXX_STANDARD_REQUIRED ON)

# Only JNI functions are exported
target_compile_options(osudroid_frame_timestamps PRIVATE -fvisibility=hidden -Wall)

target_link_libraries(osudroid_frame_timestamps EGL log)
