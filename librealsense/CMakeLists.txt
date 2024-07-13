# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.6)

include("../config.cmake")

#[[set(LRS_ROOT_DIR "${CMAKE_CURRENT_LIST_DIR}/../../")
set(LRS_ANDROID_ROOT_DIR ${CMAKE_CURRENT_LIST_DIR})

message(STATUS "librealsense root dir: ${LRS_ROOT_DIR}")
message(STATUS "librealsense Android root dir: ${LRS_ANDROID_ROOT_DIR}")]]
message(STATUS "librealsense sub: ${LRS_ANDROID_ROOT_DIR}/build-${ANDROID_ABI}")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${LRS_ANDROID_ROOT_DIR}/build/jniLibs/${ANDROID_ABI})
add_subdirectory(${LRS_ROOT_DIR} ${LRS_ANDROID_ROOT_DIR}/build-${ANDROID_ABI})
#include_directories(${LRS_ROOT_DIR}/include)