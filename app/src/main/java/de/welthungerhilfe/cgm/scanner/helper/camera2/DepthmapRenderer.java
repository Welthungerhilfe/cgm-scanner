/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.welthungerhilfe.cgm.scanner.helper.camera2;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.opengl.GLES20;
import android.os.Build;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/** Renders a point cloud. */
public class DepthmapRenderer {
  private static final String TAG = "DepthmapRenderer";

  // Shader names.
  private static final String VERTEX_SHADER_NAME = "shaders/depthmap.vert";
  private static final String FRAGMENT_SHADER_NAME = "shaders/depthmap.frag";

  private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
  private static final int FLOATS_PER_POINT = 3; // X,Y,Z.
  private static final int BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT;

  private String depthCameraId = "0";
  private int depthWidth = 240;
  private int depthHeight = 180;
  private int programName;
  private int colorAttribute;
  private int positionAttribute;
  private int pointSizeUniform;
  private int screenOrientationUniform;

  private int numPoints = 0;

  private FloatBuffer verticesBuffer = null;
  private FloatBuffer colorBuffer = null;

  /**
   * Allocates and initializes OpenGL resources needed by the plane renderer.
   *
   * @param context Needed to access shader source.
   */
  public void createOnGlThread(Context context) throws IOException {
    ShaderUtil.checkGLError(TAG, "buffer alloc");

    int vertexShader =
            ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
    int passthroughShader =
            ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

    programName = GLES20.glCreateProgram();
    GLES20.glAttachShader(programName, vertexShader);
    GLES20.glAttachShader(programName, passthroughShader);
    GLES20.glLinkProgram(programName);
    GLES20.glUseProgram(programName);

    ShaderUtil.checkGLError(TAG, "program");

    colorAttribute = GLES20.glGetAttribLocation(programName, "a_Color");
    positionAttribute = GLES20.glGetAttribLocation(programName, "a_Position");
    pointSizeUniform = GLES20.glGetUniformLocation(programName, "u_PointSize");
    screenOrientationUniform = GLES20.glGetUniformLocation(programName, "u_Rotation");
    ShaderUtil.checkGLError(TAG, "program  params");
  }

  public void initCamera(Context context) {
    depthWidth = 240;
    depthHeight = 180;

    // Note 10, Note 10 5G
    if (Build.DEVICE.toUpperCase().startsWith("D1")) { depthWidth = 640; depthHeight = 480; }

    // Note 10+, Note 10+ 5G
    if (Build.DEVICE.toUpperCase().startsWith("D2")) { depthWidth = 640; depthHeight = 480; }
    if (Build.DEVICE.toUpperCase().startsWith("SC-01M")) { depthWidth = 640; depthHeight = 480; }
    if (Build.DEVICE.toUpperCase().startsWith("SCV45")) { depthWidth = 640; depthHeight = 480; }

    // S20, S20 5G
    if (Build.DEVICE.toUpperCase().startsWith("SC-51A")) { depthWidth = 640; depthHeight = 480; }
    if (Build.DEVICE.toUpperCase().startsWith("SCG01")) { depthWidth = 640; depthHeight = 480; }
    if (Build.DEVICE.toUpperCase().startsWith("X1")) { depthWidth = 640; depthHeight = 480; }

    // S20+, S20+ 5G
    if (Build.DEVICE.toUpperCase().startsWith("Y2")) { depthWidth = 640; depthHeight = 480; }

    // S20 Ultra
    if (Build.DEVICE.toUpperCase().startsWith("Z3")) { depthWidth = 640; depthHeight = 480; }

    try {
      CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      for (String cameraId : manager.getCameraIdList()) {
        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
        Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null) {
          if (facing == CameraCharacteristics.LENS_FACING_BACK) {
            int[] ch = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            if (ch != null) {
              for (int c : ch) {
                if (c == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) {
                  depthCameraId = cameraId;
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void update(float[] depth, float[] colors) {
    numPoints = 0;
    int index = 0;
    int input = 0;
    float[] arrayPos = new float[depth.length * FLOATS_PER_POINT];
    float[] arrayRGB = new float[colors.length * FLOATS_PER_POINT];

    //generate points
    for (int y = 0; y < depthHeight; y++) {
      for (int x = 0; x < depthWidth; x++) {
        if (depth[input] > 0) {
          arrayPos[index + 0] = 2.0f * (x + 0.5f) / (float)depthWidth - 1.0f;
          arrayPos[index + 1] = -2.0f * (y + 0.5f) / (float)depthHeight + 1.0f;
          arrayPos[index + 2] = depth[input];
          arrayRGB[index + 0] = colors[input] * (depth[input] + 1.0f);
          index += 3;
          numPoints++;
        }
        input++;
      }
    }

    //prepare left points for render
    ByteBuffer buffer = ByteBuffer.allocateDirect(depth.length * BYTES_PER_POINT);
    buffer.order(ByteOrder.nativeOrder());
    verticesBuffer = buffer.asFloatBuffer();
    verticesBuffer.put(arrayPos);
    verticesBuffer.position(0);

    //prepare colors for render
    buffer = ByteBuffer.allocateDirect(colors.length * BYTES_PER_POINT);
    buffer.order(ByteOrder.nativeOrder());
    colorBuffer = buffer.asFloatBuffer();
    colorBuffer.put(arrayRGB);
    colorBuffer.position(0);
  }

  /**
   * Renders the point cloud. ARCore point cloud is given in world space.
   *
   */
  public synchronized void draw(Activity context, float zoom) {

    if (verticesBuffer == null)
      return;

    ShaderUtil.checkGLError(TAG, "Before draw");

    Point size = new Point();
    context.getWindowManager().getDefaultDisplay().getSize(size);
    float x = zoom * size.x / (float)depthWidth;
    float y = zoom * size.y / (float)depthHeight;

    GLES20.glClearColor(0f, 0f, 0f, 1.0f);
    if (size.x > size.y) {
      float aspect = size.y / (float)size.x * (float)depthWidth / (float)depthHeight;
      int width = (int) (size.x * aspect * zoom);
      int height = (int) (size.y * zoom);
      GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
      GLES20.glScissor((size.x - width) / 2, (size.y - height) / 2, width, height);
      GLES20.glViewport((size.x - width) / 2, (size.y - height) / 2, width, height);
    } else {
      float aspect = size.x / (float)size.y * (float)depthWidth / (float)depthHeight;
      int width = (int) (size.x * zoom);
      int height = (int) (size.y * aspect * zoom);
      GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
      GLES20.glScissor((size.x - width) / 2, (size.y - height) / 2, width, height);
      GLES20.glViewport((size.x - width) / 2, (size.y - height) / 2, width, height);
    }

    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    GLES20.glUseProgram(programName);
    GLES20.glEnableVertexAttribArray(colorAttribute);
    GLES20.glVertexAttribPointer(colorAttribute, FLOATS_PER_POINT, GLES20.GL_FLOAT, false, BYTES_PER_POINT, colorBuffer);
    GLES20.glEnableVertexAttribArray(positionAttribute);
    GLES20.glVertexAttribPointer(positionAttribute, FLOATS_PER_POINT, GLES20.GL_FLOAT, false, BYTES_PER_POINT, verticesBuffer);
    GLES20.glUniform1f(pointSizeUniform, Math.max(x, y));

    int rotation = context.getWindowManager().getDefaultDisplay().getRotation();
    switch (rotation) {
      case Surface.ROTATION_0:
        GLES20.glUniform1f(screenOrientationUniform, 0);
        break;
      case Surface.ROTATION_90:
        GLES20.glUniform1f(screenOrientationUniform, -90);
        break;
      case Surface.ROTATION_180:
        GLES20.glUniform1f(screenOrientationUniform, 180);
        break;
      case Surface.ROTATION_270:
        GLES20.glUniform1f(screenOrientationUniform, 90);
        break;
    }

    GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints);
    GLES20.glDisableVertexAttribArray(positionAttribute);

    GLES20.glColorMask(true, true, true, true);
    GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    GLES20.glScissor(0, 0, size.x, size.y);
    GLES20.glViewport(0, 0, size.x, size.y);
    GLES20.glClearColor(0, 0, 0, 1);

    ShaderUtil.checkGLError(TAG, "Draw");
  }

  public String getDepthCameraId() { return depthCameraId; }

  public int getDepthWidth() {
    return depthWidth;
  }

  public int getDepthHeight() {
    return depthHeight;
  }
}
