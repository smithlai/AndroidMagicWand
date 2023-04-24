package com.example.androidmagicwand.opengl

import android.opengl.GLES30
import android.util.Log
import com.example.androidmagicwand.opengl.myRenderer.Companion.LoadShader
import com.example.androidmagicwand.opengl.myRenderer.Companion.checkGlError
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Created by Seker on 7/2/2015.
 *
 *
 * This code actually will draw a cube.
 *
 * Some of the code is used from https://github.com/christopherperry/cube-rotation
 * and changed up to opengl 3.0
 */
class XYZGLObject {
    private val mProgramObject: Int
    private var mMVPMatrixHandle = 0
    private var mColorHandle = 0
    private val mVertices: FloatBuffer
    private val mAxis: FloatBuffer

    //initial size of the cube.  set here, so it is easier to change later.
    var size = 0.4f
    private val AXIS_VERTICES = floatArrayOf( // X軸
        0.0f, 0.0f, 0.0f,
        10f, 0.0f, 0.0f,
        // Y軸
        0.0f, 0.0f, 0.0f,
        0.0f, 10f, 0.0f,
        // Z軸
        0.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 10f
    )
    //this is the initial data, which will need to translated into the mVertices variable in the consturctor.
    var mVerticesData =
        floatArrayOf( ////////////////////////////////////////////////////////////////////
            // FRONT
            ////////////////////////////////////////////////////////////////////
            // Triangle 1
            -size, size, size,  // top-left
            -size, -size, size,  // bottom-left
            size, -size, size,  // bottom-right
            // Triangle 2
            size, -size, size,  // bottom-right
            size, size, size,  // top-right
            -size, size, size,  // top-left
            ////////////////////////////////////////////////////////////////////
            // BACK
            ////////////////////////////////////////////////////////////////////
            // Triangle 1
            -size, size, -size,  // top-left
            -size, -size, -size,  // bottom-left
            size, -size, -size,  // bottom-right
            // Triangle 2
            size, -size, -size,  // bottom-right
            size, size, -size,  // top-right
            -size, size, -size,  // top-left
            ////////////////////////////////////////////////////////////////////
            // LEFT
            ////////////////////////////////////////////////////////////////////
            // Triangle 1
            -size, size, -size,  // top-left
            -size, -size, -size,  // bottom-left
            -size, -size, size,  // bottom-right
            // Triangle 2
            -size, -size, size,  // bottom-right
            -size, size, size,  // top-right
            -size, size, -size,  // top-left
            ////////////////////////////////////////////////////////////////////
            // RIGHT
            ////////////////////////////////////////////////////////////////////
            // Triangle 1
            size, size, -size,  // top-left
            size, -size, -size,  // bottom-left
            size, -size, size,  // bottom-right
            // Triangle 2
            size, -size, size,  // bottom-right
            size, size, size,  // top-right
            size, size, -size,  // top-left
            ////////////////////////////////////////////////////////////////////
            // TOP
            ////////////////////////////////////////////////////////////////////
            // Triangle 1
            -size, size, -size,  // top-left
            -size, size, size,  // bottom-left
            size, size, size,  // bottom-right
            // Triangle 2
            size, size, size,  // bottom-right
            size, size, -size,  // top-right
            -size, size, -size,  // top-left
            ////////////////////////////////////////////////////////////////////
            // BOTTOM
            ////////////////////////////////////////////////////////////////////
            // Triangle 1
            -size, -size, -size,  // top-left
            -size, -size, size,  // bottom-left
            size, -size, size,  // bottom-right
            // Triangle 2
            size, -size, size,  // bottom-right
            size, -size, -size,  // top-right
            -size, -size, -size // top-left
        )
    var colorcyan = myColor.cyan()
    var colorblue = myColor.blue()
    var colorred = myColor.red()
    var colorgray = myColor.gray()
    var colorgreen = myColor.green()
    var coloryellow = myColor.yellow()

    //vertex shader code
    var vShaderStr = """#version 300 es 			  
uniform mat4 uMVPMatrix;     
in vec4 vPosition;           
void main()                  
{                            
   gl_Position = uMVPMatrix * vPosition;  
}                            
"""

    //fragment shader code.
    var fShaderStr = """#version 300 es		 			          	
precision mediump float;					  	
uniform vec4 vColor;	 			 		  	
out vec4 fragColor;	 			 		  	
void main()                                  
{                                            
  fragColor = vColor;                    	
}                                            
"""
    var TAG = "XYZ"

    //finally some methods
    //constructor
    init {
        //first setup the mVertices correctly.
        mVertices = ByteBuffer
            .allocateDirect(mVerticesData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(mVerticesData)
        mVertices.position(0)

        mAxis = ByteBuffer
            .allocateDirect(AXIS_VERTICES.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(AXIS_VERTICES)
        mAxis.position(0)

        //setup the shaders
        val vertexShader: Int
        val fragmentShader: Int
        val programObject: Int
        val linked = IntArray(1)

        // Load the vertex/fragment shaders
        vertexShader = LoadShader(GLES30.GL_VERTEX_SHADER, vShaderStr)
        fragmentShader = LoadShader(GLES30.GL_FRAGMENT_SHADER, fShaderStr)

        // Create the program object
        programObject = GLES30.glCreateProgram()
        if (programObject == 0) {
            Log.e(TAG, "So some kind of error, but what?")
            throw IllegalStateException("glCreateProgram初始化失败")
        }
        GLES30.glAttachShader(programObject, vertexShader)
        GLES30.glAttachShader(programObject, fragmentShader)

        // Bind vPosition to attribute 0
        GLES30.glBindAttribLocation(programObject, 0, "vPosition")

        // Link the program
        GLES30.glLinkProgram(programObject)

        // Check the link status
        GLES30.glGetProgramiv(programObject, GLES30.GL_LINK_STATUS, linked, 0)
        if (linked[0] == 0) {
            Log.e(TAG, "Error linking program:")
            Log.e(TAG, GLES30.glGetProgramInfoLog(programObject))
            GLES30.glDeleteProgram(programObject)
            throw IllegalStateException("glGetProgramiv初始化失败")
        }

        // Store the program object
        mProgramObject = programObject

        //now everything is setup and ready to draw.
    }

    fun draw(mvpMatrix: FloatArray?) {
        // Use the program object
        GLES30.glUseProgram(mProgramObject)

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgramObject, "uMVPMatrix")
        checkGlError("glGetUniformLocation")

        // get handle to fragment shader's vColor member
        mColorHandle = GLES30.glGetUniformLocation(mProgramObject, "vColor")


        // Apply the projection and view transformation
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
        checkGlError("glUniformMatrix4fv")
        val VERTEX_POS_INDX = 0
        mVertices.position(VERTEX_POS_INDX) //just in case.  We did it already though.

        //add all the points to the space, so they can be correct by the transformations.
        //would need to do this even if there were no transformations actually.
        GLES30.glVertexAttribPointer(
            VERTEX_POS_INDX, 3, GLES30.GL_FLOAT,
            false, 0, mVertices
        )
        GLES30.glEnableVertexAttribArray(VERTEX_POS_INDX)

        //Now we are ready to draw the cube finally.
        var startPos = 0
        val verticesPerface = 6

        //draw front face
        GLES30.glUniform4fv(mColorHandle, 1, colorblue, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, startPos, verticesPerface)
        startPos += verticesPerface

        //draw back face
        GLES30.glUniform4fv(mColorHandle, 1, colorcyan, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, startPos, verticesPerface)
        startPos += verticesPerface

        //draw left face
        GLES30.glUniform4fv(mColorHandle, 1, colorred, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, startPos, verticesPerface)
        startPos += verticesPerface

        //draw right face
        GLES30.glUniform4fv(mColorHandle, 1, colorgray, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, startPos, verticesPerface)
        startPos += verticesPerface

        //draw top face
        GLES30.glUniform4fv(mColorHandle, 1, colorgreen, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, startPos, verticesPerface)
        startPos += verticesPerface

        //draw bottom face
        GLES30.glUniform4fv(mColorHandle, 1, coloryellow, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, startPos, verticesPerface)
        //last face, so no need to increment.


        mAxis.position(VERTEX_POS_INDX) //just in case.  We did it already though.

        //add all the points to the space, so they can be correct by the transformations.
        //would need to do this even if there were no transformations actually.
        GLES30.glVertexAttribPointer(
            VERTEX_POS_INDX, 3, GLES30.GL_FLOAT,
            false, 0, mAxis
        )
        GLES30.glEnableVertexAttribArray(VERTEX_POS_INDX)

        GLES30.glUniform4fv(mColorHandle, 1, coloryellow, 0)
        GLES30.glDrawArrays(GLES30.GL_LINES, 0, 2)
        GLES30.glUniform4fv(mColorHandle, 1, coloryellow, 0)
        GLES30.glDrawArrays(GLES30.GL_LINES, 2, 4)
        GLES30.glUniform4fv(mColorHandle, 1, coloryellow, 0)
        GLES30.glDrawArrays(GLES30.GL_LINES, 4, 6)
    }
}