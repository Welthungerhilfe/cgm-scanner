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
package de.welthungerhilfe.cgm.scanner.utils;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Size;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class RenderToTexture {

    private static final String BASE_FRAGMENT =
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "varying vec2 textureCoordinate;\n"
                    + "uniform samplerExternalOES vTexture;\n"
                    + "void main() {\n"
                    + "    gl_FragColor = texture2D(vTexture, textureCoordinate);\n"
                    + "}";

    private static final String BASE_VERTEX =
            "attribute vec4 vPosition;\n"
                    + "attribute vec2 vCoord;\n"
                    + "uniform mat4 vMatrix;\n"
                    + "varying vec2 textureCoordinate;\n"
                    + "void main(){\n"
                    + "    gl_Position = vPosition;\n"
                    + "    textureCoordinate = vCoord;\n"
                    + "}";

    private static final float[] POS = {-1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f};

    private static final float[] COORD = {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f};

    private int mFBO;
    private int mProgram;
    private int mTextureCoord;
    private int mTexturePos;
    private FloatBuffer mVerBuffer;
    private FloatBuffer mTexBuffer;

    public RenderToTexture() {
        mFBO = -1;
    }

    public void init() {

        // Create shader program
        mProgram = ShaderUtils.getProgram(BASE_VERTEX, BASE_FRAGMENT);
        mTexturePos = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mTextureCoord = GLES20.glGetAttribLocation(mProgram, "vCoord");

        // Initialize the size of the vertex buffer.
        ByteBuffer byteBufferForVer = ByteBuffer.allocateDirect(32);
        byteBufferForVer.order(ByteOrder.nativeOrder());
        mVerBuffer = byteBufferForVer.asFloatBuffer();
        mVerBuffer.put(POS);
        mVerBuffer.position(0);

        // Initialize the size of the texture buffer.
        ByteBuffer byteBufferForTex = ByteBuffer.allocateDirect(32);
        byteBufferForTex.order(ByteOrder.nativeOrder());
        mTexBuffer = byteBufferForTex.asFloatBuffer();
        mTexBuffer.put(COORD);
        mTexBuffer.position(0);
    }

    public Bitmap renderData(int texture, Size resolution) {
        if (mFBO == -1) {
            mFBO = ShaderUtils.createFBO(resolution.getWidth(), resolution.getHeight());
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFBO);
        GLES20.glViewport(0, 0, resolution.getWidth(), resolution.getHeight());
        onDrawTexture(texture);
        Bitmap output = BitmapUtils.getBitmap(resolution.getWidth(), resolution.getHeight());
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return output;
    }

    private void onDrawTexture(int texture) {

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);

        GLES20.glUseProgram(mProgram);

        // Set the texture ID.
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);

        // Set the vertex.
        GLES20.glEnableVertexAttribArray(mTexturePos);
        GLES20.glVertexAttribPointer(mTexturePos, 2, GLES20.GL_FLOAT, false, 0, mVerBuffer);

        // Set the texture coordinates.
        GLES20.glEnableVertexAttribArray(mTextureCoord);
        GLES20.glVertexAttribPointer(mTextureCoord, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);

        // Number of vertices.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mTexturePos);
        GLES20.glDisableVertexAttribArray(mTextureCoord);

        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
}
