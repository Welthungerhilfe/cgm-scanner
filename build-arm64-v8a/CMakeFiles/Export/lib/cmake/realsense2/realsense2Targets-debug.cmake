#----------------------------------------------------------------
# Generated CMake target import file for configuration "Debug".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "realsense2::rsutils" for configuration "Debug"
set_property(TARGET realsense2::rsutils APPEND PROPERTY IMPORTED_CONFIGURATIONS DEBUG)
set_target_properties(realsense2::rsutils PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_DEBUG "CXX"
  IMPORTED_LOCATION_DEBUG "${_IMPORT_PREFIX}/lib/librsutils.a"
  )

list(APPEND _IMPORT_CHECK_TARGETS realsense2::rsutils )
list(APPEND _IMPORT_CHECK_FILES_FOR_realsense2::rsutils "${_IMPORT_PREFIX}/lib/librsutils.a" )

# Import target "realsense2::realsense-file" for configuration "Debug"
set_property(TARGET realsense2::realsense-file APPEND PROPERTY IMPORTED_CONFIGURATIONS DEBUG)
set_target_properties(realsense2::realsense-file PROPERTIES
  IMPORTED_LINK_INTERFACE_LANGUAGES_DEBUG "C;CXX"
  IMPORTED_LOCATION_DEBUG "${_IMPORT_PREFIX}/lib/librealsense-file.a"
  )

list(APPEND _IMPORT_CHECK_TARGETS realsense2::realsense-file )
list(APPEND _IMPORT_CHECK_FILES_FOR_realsense2::realsense-file "${_IMPORT_PREFIX}/lib/librealsense-file.a" )

# Import target "realsense2::realsense2" for configuration "Debug"
set_property(TARGET realsense2::realsense2 APPEND PROPERTY IMPORTED_CONFIGURATIONS DEBUG)
set_target_properties(realsense2::realsense2 PROPERTIES
  IMPORTED_LOCATION_DEBUG "${_IMPORT_PREFIX}/lib/librealsense2.so"
  IMPORTED_SONAME_DEBUG "librealsense2.so"
  )

list(APPEND _IMPORT_CHECK_TARGETS realsense2::realsense2 )
list(APPEND _IMPORT_CHECK_FILES_FOR_realsense2::realsense2 "${_IMPORT_PREFIX}/lib/librealsense2.so" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
