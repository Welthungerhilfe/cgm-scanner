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

package de.welthungerhilfe.cgm.scanner.helper.arcore.render;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.opengl.GLES20;
import android.util.Log;
import android.util.Size;

import com.google.ar.core.PointCloud;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/** Renders a point cloud. */
public class OcclusionRenderer {
  private static final String TAG = PointCloud.class.getSimpleName();

  // Shader names.
  private static final String VERTEX_SHADER_NAME = "shaders/occlusion.vert";
  private static final String FRAGMENT_SHADER_NAME = "shaders/occlusion.frag";

  private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
  private static final int FLOATS_PER_POINT = 3; // X,Y,Z.
  private static final int BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT;

  private int depthWidth = -1;
  private int depthHeight = -1;
  private int programName;
  private int positionAttribute;
  private int pointSizeUniform;

  private int numPoints = 0;

  private FloatBuffer verticesBuffer = null;

  public OcclusionRenderer() {}

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

    positionAttribute = GLES20.glGetAttribLocation(programName, "a_Position");
    pointSizeUniform = GLES20.glGetUniformLocation(programName, "u_PointSize");

    ShaderUtil.checkGLError(TAG, "program  params");
  }

  public ArrayList<String> getResolutions(Context context, String cameraId) {
    ArrayList<String> output = new ArrayList<>();
    try {
      CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
      for (Size s : characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.DEPTH16)) {
        output.add(s.getWidth() + "x" + s.getHeight());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return output;
  }

  public void initCamera(Context context, String cameraId, int index) {
    boolean ok = false;
    try {
      int current = 0;
      CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
      for (Size s : characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.DEPTH16)) {
        depthWidth = s.getWidth();
        depthHeight = s.getHeight();
        ok = true;
        if (current == index)
          break;
        else;
          current++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (!ok) {
      Log.e("ARCoreApp", "Depth sensor not found!");
      System.exit(1);
    }
  }

  public synchronized void update(float[] data) {
    numPoints = 0;
    int index = 0;
    int input = 0;
    float[] array = new float[data.length * FLOATS_PER_POINT];
    for (int y = 0; y < depthHeight; y++) {
      for (int x = 0; x < depthWidth; x++) {
        if (data[input] > 0) {
          array[index++] = 2.0f * (x + 0.5f) / (float)depthWidth - 1.0f;
          array[index++] = -2.0f * (y + 0.5f) / (float)depthHeight + 1.0f;
          array[index++] = data[input];
          numPoints++;
        }
        input++;
      }
    }
    ByteBuffer buffer = ByteBuffer.allocateDirect(data.length * BYTES_PER_POINT);
    buffer.order(ByteOrder.nativeOrder());
    verticesBuffer = buffer.asFloatBuffer();
    verticesBuffer.put(array);
    verticesBuffer.position(0);
  }

  /**
   * Renders the point cloud. ARCore point cloud is given in world space.
   *
   */
  public synchronized void draw(boolean render) {

    if (verticesBuffer == null)
      return;

    ShaderUtil.checkGLError(TAG, "Before draw");

    if (!render)
      GLES20.glColorMask(false, false, false, false);
    GLES20.glUseProgram(programName);
    GLES20.glEnableVertexAttribArray(positionAttribute);
    GLES20.glVertexAttribPointer(positionAttribute, FLOATS_PER_POINT, GLES20.GL_FLOAT, false, BYTES_PER_POINT, verticesBuffer);
    GLES20.glUniform1f(pointSizeUniform, 125.0f);

    GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints);
    GLES20.glDisableVertexAttribArray(positionAttribute);
    GLES20.glColorMask(true, true, true, true);

    ShaderUtil.checkGLError(TAG, "Draw");
  }

  public int getDepthWidth() {
    return depthWidth;
  }

  public int getDepthHeight() {
    return depthHeight;
  }
}
