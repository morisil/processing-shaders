/*
 * processing-shaders - processing + maven + GLSL + kinect examples
 *
 * Copyright (C) 2019  Kazimierz Pogoda
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.xemantic.processing.shaders;

import com.jogamp.common.nio.Buffers;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.openkinect.processing.Kinect;
import processing.core.PApplet;
import processing.opengl.PGL;
import processing.opengl.PShader;

/**
 * Adapted from this post by Anderson Sudario:
 *
 * https://forum.processing.org/two/discussion/17578/change-colors-of-points-data-from-kinect-using-shaders
 */
public class Kinect1ColorPointCloud extends PApplet {

  // Kinect Library object
  private Kinect kinect;

  // Angle for rotation
  private float a = PI;

  private PShader sh;

  private int vertLocId;

  private int colorLocId;
  private float [] colors = new float[640 * 480 * 4];

  public static void main(String ... args) {
    PApplet.main(Kinect1ColorPointCloud.class.getName());
  }

  @Override
  public void settings() {
    fullScreen(P3D);
  }

  @Override
  public void setup() {
    kinect = new Kinect(this);
    kinect.initDepth();
    kinect.initVideo();
    kinect.enableMirror(true);

    //load shaders
    sh = loadShader(
        "src/main/glsl/cloud/frag.glsl",
        "src/main/glsl/cloud/vert.glsl"
    ); //only one change in frag.glsl
    PGL pgl = beginPGL();

    IntBuffer intBuffer = IntBuffer.allocate(2);
    pgl.genBuffers(2, intBuffer);

    //memory location of the VBO
    vertLocId = intBuffer.get(0);
    colorLocId = intBuffer.get(1);
    endPGL();
  }

  void buildColors() {
    int textureOffsetY = kinect.width * 35;
    for (int i = 0; i < kinect.width*kinect.height - textureOffsetY; i += 1) { //skip if wanted
      int c = kinect.getVideoImage().pixels[i + textureOffsetY];
      colors[i * 4]     = (c >> 16 & 0xff) / 100f;
      colors[i * 4 + 1] = (c >> 8 & 0xff) / 100f;
      colors[i * 4 + 2] = (c & 0xff) / 100f;
      colors[i * 4 + 3] = 1;
    }
  }

  @Override
  public void draw() {
    background(0);

    pushMatrix();
    translate(width / 2f, height / 2f, 600);
    scale(400);
    rotateY(a);

    int vertData = kinect.width * kinect.height;
    FloatBuffer depthPositions =  kinect.getDephToWorldPositions();

    buildColors();
    FloatBuffer colorBuffer = Buffers.newDirectFloatBuffer(640 * 480 * 4);
    colorBuffer.put(colors, 0, 640 * 480 * 4);
    colorBuffer.rewind();

    //openGL
    PGL pgl = beginPGL();
    sh.bind();

    //vertex
    int vertLoc = pgl.getAttribLocation(sh.glProgram, "vertex");
    //colors
    int colorLoc = pgl.getAttribLocation(sh.glProgram, "color");

    pgl.enableVertexAttribArray(vertLoc);
    pgl.enableVertexAttribArray(colorLoc);

    pgl.bindBuffer(PGL.ARRAY_BUFFER, vertLocId);
    pgl.bufferData(PGL.ARRAY_BUFFER, Float.BYTES * vertData *3, depthPositions, PGL.DYNAMIC_DRAW);
    pgl.vertexAttribPointer(vertLoc, 3, PGL.FLOAT, false, Float.BYTES * 3, 0);

    final int colorStride  =  4 * Float.BYTES;
    final int colorOffset  =  0;

    pgl.bindBuffer(PGL.ARRAY_BUFFER, colorLocId);
    pgl.bufferData(PGL.ARRAY_BUFFER, Float.BYTES * colors.length, colorBuffer, PGL.DYNAMIC_DRAW);
    pgl.vertexAttribPointer(colorLoc, 4, PGL.FLOAT, false, colorStride, colorOffset);

    pgl.bindBuffer(PGL.ARRAY_BUFFER, 0);

    //draw the XYZ depth camera points
    pgl.drawArrays(PGL.POINTS, 0, vertData);

    //clean up the vertex buffers
    pgl.disableVertexAttribArray(vertLoc);
    pgl.disableVertexAttribArray(colorLoc);

    sh.unbind();
    endPGL();

    popMatrix();

    fill(255, 0, 0);
    text(frameRate, 50, 50);

    // Rotate
    a += sin(millis()/1000.0f) / 500.0f;
  }

}
