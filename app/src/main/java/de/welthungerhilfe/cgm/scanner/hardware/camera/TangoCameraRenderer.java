package de.welthungerhilfe.cgm.scanner.hardware.camera;

/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.projecttango.tangosupport.TangoSupport;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import de.welthungerhilfe.cgm.scanner.hardware.gpu.GLSL;

/**
 * Renderer object for our GLSurfaceView with the Camera Preview from Tango.
 * <p>
 * Do not call any methods here directly from another thread -- use the
 * GLSurfaceView#queueEvent() call.
 */
public class TangoCameraRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = TangoCameraRenderer.class.getSimpleName();

    // width/height of the incoming camera preview frames
    private int mIncomingWidth;
    private int mIncomingHeight;

    private static final int INVALID_TEXTURE_ID = -1;

    private final float[] textureCoords0 =
            new float[]{1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f};

    /**
     * A small callback to allow the caller to introduce application-specific code to be executed
     * in the OpenGL thread.
     */
    public interface RenderCallback {
        void preRender();
    }

    private FloatBuffer mVertex;
    private FloatBuffer mTexCoord;
    private ShortBuffer mIndices;
    private int[] mVbos;
    private int[] mTextures = new int[1];
    private int mProgram;
    private RenderCallback mRenderCallback;

    /**
     * Constructs CameraSurfaceRenderer.
     * <p>
     * @param callback A small callback to allow the caller to introduce application-specific code to be executed
     */

    public TangoCameraRenderer(RenderCallback callback) {
        mRenderCallback = callback;
        mIncomingWidth = mIncomingHeight = -1;

        mTextures[0] = 0;
        // Vertex positions.
        float[] vtmp = { 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f };
        // Vertex texture coords.
        float[] ttmp = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };
        // Indices.
        short[] itmp = {0, 1, 2, 3};
        mVertex = ByteBuffer.allocateDirect(vtmp.length * Float.SIZE / 8).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
        mVertex.put(vtmp);
        mVertex.position(0);
        mTexCoord = ByteBuffer.allocateDirect(ttmp.length * Float.SIZE / 8).order(
                ByteOrder.nativeOrder()).asFloatBuffer();
        mTexCoord.put(ttmp);
        mTexCoord.position(0);
        mIndices = ByteBuffer.allocateDirect(itmp.length * Short.SIZE / 8).order(
                ByteOrder.nativeOrder()).asShortBuffer();
        mIndices.put(itmp);
        mIndices.position(0);
    }

    public void updateColorCameraTextureUv(int rotation){
        float[] textureCoords =
                TangoSupport.getVideoOverlayUVBasedOnDisplayRotation(textureCoords0, rotation);
        setTextureCoords(textureCoords);
    }

    private void setTextureCoords(float[] textureCoords) {
        mTexCoord.put(textureCoords);
        mTexCoord.position(0);
        if (mVbos != null) {
            // Bind to texcoord buffer.
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbos[1]);
            // Populate it.
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * 2 * Float
                    .SIZE / 8, mTexCoord, GLES20.GL_STATIC_DRAW); // texcoord of floats.
        }
    }

    /**
     * Records the size of the incoming camera preview frames.
     * <p>
     * It's not clear whether this is guaranteed to execute before or after onSurfaceCreated(),
     * so we assume it could go either way.  (Fortunately they both run on the same thread,
     * so we at least know that they won't execute concurrently.)
     */
    public void setCameraPreviewSize(int width, int height) {
        Log.d(TAG, "setCameraPreviewSize");
        mIncomingWidth = width;
        mIncomingHeight = height;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig eglConfig) {
        Log.d(TAG, "onSurfaceCreated");

        createTextures();
        createCameraVbos();
        GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
        String vss = "attribute vec2 vPosition;\n" +
                "attribute vec2 vTexCoord;\n" +
                "varying vec2 texCoord;\n" +
                "void main() {\n" +
                "  texCoord = vTexCoord;\n" +
                "  gl_Position = vec4(vPosition.x, vPosition.y, 0.0, 1.0);\n" +
                "}";
        String fss = "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "varying vec2 texCoord;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(sTexture,texCoord);\n" +
                "}";
        mProgram = GLSL.getProgram(vss, fss);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        setCameraPreviewSize(width,height);
        Log.d(TAG, "onSurfaceChanged " + width + "x" + height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Call application-specific code that needs to run on the OpenGL thread.
        // This is where updateTexImage is called in contrast to grafikas Show+Capture Camera example.
        mRenderCallback.preRender();

        if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
            // Texture size isn't set yet.  This is only used for the filters, but to be
            // safe we can just skip drawing while we wait for the various races to resolve.
            // (This seems to happen if you toggle the screen off/on with power button.)
            Log.i(TAG, "Drawing before incoming texture size set; skipping");
            return;
        }

        GLES20.glUseProgram(mProgram);

        // Don't write depth buffer because we want to draw the camera as background.
        GLES20.glDepthMask(false);

        int ph = GLES20.glGetAttribLocation(mProgram, "vPosition");
        int tch = GLES20.glGetAttribLocation(mProgram, "vTexCoord");
        int th = GLES20.glGetUniformLocation(mProgram, "sTexture");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);
        GLES20.glUniform1i(th, 0);

        GLES20.glEnableVertexAttribArray(ph);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbos[0]);
        GLES20.glVertexAttribPointer(ph, 2, GLES20.GL_FLOAT, false, 4 * 2, 0);

        GLES20.glEnableVertexAttribArray(tch);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbos[1]);
        GLES20.glVertexAttribPointer(tch, 2, GLES20.GL_FLOAT, false, 4 * 2, 0);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mVbos[2]);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, 4, GLES20.GL_UNSIGNED_SHORT, 0);

        // Unbind.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    private void createTextures() {
        mTextures = new int[1];
        GLES20.glGenTextures(1, mTextures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    }

    /**
     * Creates and populates vertex buffer objects for rendering the camera.
     */
    private void createCameraVbos() {
        mVbos = new int[3];
        // Generate three buffers: vertex buffer, texture buffer and index buffer.
        GLES20.glGenBuffers(3, mVbos, 0);
        // Bind to vertex buffer.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbos[0]);
        // Populate it.
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertex.capacity() * Float.SIZE / 8,
                mVertex, GLES20.GL_STATIC_DRAW); // 4 2D vertex of floats.

        // Bind to texture buffer.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbos[1]);
        // Populate it.
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mTexCoord.capacity() * Float.SIZE / 8,
                mTexCoord, GLES20.GL_STATIC_DRAW); // 4 2D texture coords of floats.

        // Bind to indices buffer.
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mVbos[2]);
        // Populate it.
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndices.capacity() * Short.SIZE / 8,
                mIndices, GLES20.GL_STATIC_DRAW); // 4 short indices.

        // Unbind buffer.
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public int getTextureId() {
        return mTextures[0];
    }
}