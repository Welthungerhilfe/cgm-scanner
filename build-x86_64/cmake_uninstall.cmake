IF(NOT EXISTS "D:/CGM/cgm-scanner/build-x86_64/install_manifest.txt")
  MESSAGE(WARNING "Cannot find install manifest: \"D:/CGM/cgm-scanner/build-x86_64/install_manifest.txt\"")
  MESSAGE(STATUS "Uninstall targets will be skipped")
ELSE(NOT EXISTS "D:/CGM/cgm-scanner/build-x86_64/install_manifest.txt")
  FILE(READ "D:/CGM/cgm-scanner/build-x86_64/install_manifest.txt" files)
  STRING(REGEX REPLACE "\n" ";" files "${files}")
  FOREACH(file ${files})
    MESSAGE(STATUS "Uninstalling \"$ENV{DESTDIR}${file}\"")
    IF(EXISTS "$ENV{DESTDIR}${file}")
      EXEC_PROGRAM(
        "D:/AndroidStudio/cmake/3.22.1/bin/cmake.exe" ARGS "-E remove \"$ENV{DESTDIR}${file}\""
        OUTPUT_VARIABLE rm_out
        RETURN_VALUE rm_retval
        )
      IF(NOT "${rm_retval}" STREQUAL 0)
        MESSAGE(FATAL_ERROR "Problem when removing \"$ENV{DESTDIR}${file}\"")
      ENDIF(NOT "${rm_retval}" STREQUAL 0)
    ELSEIF(NOT "${CMAKE_VERSION}" STRLESS "2.8.1")
      IF(IS_SYMLINK "$ENV{DESTDIR}${file}")
        EXEC_PROGRAM(
          "D:/AndroidStudio/cmake/3.22.1/bin/cmake.exe" ARGS "-E remove \"$ENV{DESTDIR}${file}\""
          OUTPUT_VARIABLE rm_out
          RETURN_VALUE rm_retval
          )
        IF(NOT "${rm_retval}" STREQUAL 0)
          MESSAGE(FATAL_ERROR "Problem when removing \"$ENV{DESTDIR}${file}\"")
        ENDIF(NOT "${rm_retval}" STREQUAL 0)
      ENDIF(IS_SYMLINK "$ENV{DESTDIR}${file}")
    ELSE(EXISTS "$ENV{DESTDIR}${file}")
      MESSAGE(STATUS "File \"$ENV{DESTDIR}${file}\" does not exist.")
    ENDIF(EXISTS "$ENV{DESTDIR}${file}")
  ENDFOREACH(file)
  execute_process(COMMAND ldconfig)
ENDIF(NOT EXISTS "D:/CGM/cgm-scanner/build-x86_64/install_manifest.txt")
