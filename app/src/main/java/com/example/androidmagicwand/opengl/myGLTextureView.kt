package com.example.androidmagicwand.opengl

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLUtils
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import javax.microedition.khronos.egl.*
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL11

/*
 * This code is borrowed from https://github.com/ykulbashian/LiquidSurface/blob/master/liquidview/src/main/java/com/mycardboarddreams/liquidsurface/GLTextureView.java
 * based on a lot of android and others saying how to do this.  But allowing everyone to keep their
 * rendereres.
 *
 * As note, while it is mostly ykulbashian code from https://github.com/ykulbashian/LiquidSurface repo
 * , I changed the renderer to myRenderer and left in my ontouchevents.
 */
class myGLTextureView : TextureView, SurfaceTextureListener {
    var mRenderer: myRenderer? = null
    private var mSurface: SurfaceTexture? = null
    private var mEglDisplay: EGLDisplay? = null
    private var mEglSurface: EGLSurface? = null
    private lateinit var mEglContext: EGLContext
    private lateinit var mEgl: EGL10
    private lateinit var mGl: GL10
    private var eglConfig: EGLConfig? = null

    private var targetFrameDurationMillis = 0
    private var surfaceHeight = 0
    private var surfaceWidth = 0
    var isRunning = false

    @get:Synchronized
    @set:Synchronized
    var isPaused = true
        set(isPaused) {
            Log.d(TAG, String.format("Setting GLTextureView paused to %s", isPaused))
            field = isPaused
        }
    private var rendererChanged = false
    private var thread: RenderThread? = null
    private var targetFps = 0

    constructor(context: Context) : super(context) {
        initialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize(context)
    }

    @Synchronized
    fun setRenderer(renderer: myRenderer?) {
        mRenderer = renderer
        rendererChanged = true
    }

    private fun initialize(context: Context) {
        targetFps = TARGET_FRAME_RATE
        surfaceTextureListener = this
    }

    private var mPreviousX = 0f
    private var mPreviousY = 0f
    override fun onTouchEvent(e: MotionEvent): Boolean {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
        val x = e.x
        val y = e.y
        when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx = x - mPreviousX
                //subtract, so the cube moves the same direction as your finger.
                //with plus it moves the opposite direction.
                mRenderer!!.x = mRenderer!!.x - dx * TOUCH_SCALE_FACTOR
                val dy = y - mPreviousY
                mRenderer!!.y = mRenderer!!.y - dy * TOUCH_SCALE_FACTOR
                Log.v(TAG, "moving?")
            }
        }
        mPreviousX = x
        mPreviousY = y
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        startThread(surface, width, height, targetFps.toFloat())
    }

    fun startThread(
        surface: SurfaceTexture?,
        width: Int,
        height: Int,
        targetFramesPerSecond: Float
    ) {
        Log.d(TAG, "Starting GLTextureView thread")
        thread = RenderThread()
        mSurface = surface
        setDimensions(width, height)
        targetFrameDurationMillis = (1f / targetFramesPerSecond * 1000).toInt()
        thread!!.start()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        setDimensions(width, height)
        mRenderer?.onSurfaceChanged(mGl, width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.e(TAG, "SurfaceTExture Destroyed call.")
        stopThread()
        return false
    }

    fun stopThread() {
        if (thread != null) {
            Log.d(TAG, "Stopping and joining GLTextureView")
            isRunning = false
            try {
                thread!!.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            thread = null
        }
    }

    private fun shouldSleep(): Boolean {
        return isPaused || mRenderer == null
    }

    private inner class RenderThread : Thread() {
        override fun run() {
            isRunning = true
            initGL()
            checkGlError()
            var lastFrameTime = System.currentTimeMillis()
            while (isRunning) {
                while (mRenderer == null) {
                    try {
                        sleep(100)
                    } catch (e: InterruptedException) {
                        // Ignore
                    }
                }
                if (rendererChanged) {
                    initializeRenderer(mRenderer)
                    rendererChanged = false
                }
                if (!shouldSleep()) {
                    lastFrameTime = System.currentTimeMillis()
                    drawSingleFrame()
                }
                try {
                    if (shouldSleep()) sleep(100) else {
                        val thisFrameTime = System.currentTimeMillis()
                        val timDiff = thisFrameTime - lastFrameTime
                        lastFrameTime = thisFrameTime
                        sleep(Math.max(10L, targetFrameDurationMillis - timDiff))
                    }
                } catch (e: InterruptedException) {
// Ignore
                }
            }
        }
    }

    @Synchronized
    private fun initializeRenderer(renderer: myRenderer?) {
        if (renderer != null && isRunning) {
            //Log.v(TAG, "initialing and startin? renderer");
            //renderer.mySetup(mGl30);
            renderer.onSurfaceCreated(mGl, eglConfig!!)
            renderer.onSurfaceChanged(mGl, surfaceWidth, surfaceHeight)
        }
    }

    @Synchronized
    private fun drawSingleFrame() {
        checkCurrent()
        if (mRenderer != null) {
            mRenderer!!.onDrawFrame(mGl)
            //Log.v(TAG, "drawing in renderer");
        }
        checkGlError()
        if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
            Log.e(TAG, "cannot swap buffers!")
        }
    }

    fun setDimensions(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
    }

    private fun checkCurrent() {
        if (mEglContext != mEgl.eglGetCurrentContext()
            || mEglSurface != mEgl.eglGetCurrentSurface(EGL10.EGL_DRAW)
        ) {
            checkEglError()
            if (!mEgl.eglMakeCurrent(
                    mEglDisplay, mEglSurface,
                    mEglSurface, mEglContext
                )
            ) {
                throw RuntimeException(
                    "eglMakeCurrent failed "
                            + GLUtils.getEGLErrorString(
                        mEgl.eglGetError()
                    )
                )
            }
            checkEglError()
        }
    }

    private fun checkEglError() {
        val error = mEgl.eglGetError()
        if (error != EGL10.EGL_SUCCESS) {
            Log.e(TAG, "EGL error = 0x" + Integer.toHexString(error))
        }
    }

    private fun checkGlError() {
        val error = mGl.glGetError()
        if (error != GL11.GL_NO_ERROR) {
            Log.e(TAG, "GL error = 0x" + Integer.toHexString(error))
        }
    }

    private fun initGL() {
        mEgl = EGLContext.getEGL() as EGL10
        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (mEglDisplay === EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException(
                "eglGetDisplay failed "
                        + GLUtils.getEGLErrorString(mEgl.eglGetError())
            )
        }
        val version = IntArray(2)
        if (!mEgl.eglInitialize(mEglDisplay, version)) {
            throw RuntimeException(
                "eglInitialize failed "
                        + GLUtils.getEGLErrorString(mEgl.eglGetError())
            )
        }
        val configsCount = IntArray(1)
        val configs = arrayOfNulls<EGLConfig>(1)
        val configSpec = intArrayOf(
            EGL10.EGL_RENDERABLE_TYPE,
            EGL_OPENGL_ES2_BIT,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16,  //was 0
            EGL10.EGL_STENCIL_SIZE, 0,
            EGL10.EGL_NONE
        )
        eglConfig = null
        require(
            mEgl.eglChooseConfig(
                mEglDisplay, configSpec, configs, 1,
                configsCount
            )
        ) {
            ("eglChooseConfig failed "
                    + GLUtils.getEGLErrorString(
                mEgl.eglGetError()
            ))
        }
        if (configsCount[0] > 0) {
            eglConfig = configs[0]
        }
        if (eglConfig == null) {
            throw RuntimeException("eglConfig not initialized")
        }
        val attrib_list = intArrayOf(
            EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE
        )
        mEglContext = mEgl.eglCreateContext(
            mEglDisplay,
            eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list
        )
        checkEglError()
        mEglSurface = mEgl.eglCreateWindowSurface(
            mEglDisplay, eglConfig, mSurface, null
        )
        checkEglError()
        if (mEglSurface == null || mEglSurface === EGL10.EGL_NO_SURFACE) {
            val error = mEgl.eglGetError()
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e(
                    TAG,
                    "eglCreateWindowSurface returned EGL10.EGL_BAD_NATIVE_WINDOW"
                )
                return
            }
            throw RuntimeException(
                "eglCreateWindowSurface failed "
                        + GLUtils.getEGLErrorString(error)
            )
        }
        if (!mEgl.eglMakeCurrent(
                mEglDisplay, mEglSurface,
                mEglSurface, mEglContext
            )
        ) {
            throw RuntimeException(
                "eglMakeCurrent failed "
                        + GLUtils.getEGLErrorString(mEgl.eglGetError())
            )
        }
        checkEglError()
        mGl = mEglContext.getGL() as GL10
        checkEglError()
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    companion object {
        private const val TARGET_FRAME_RATE = 55
        private const val EGL_OPENGL_ES2_BIT = 4
        private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
        private const val TAG = "RenderThread"

        //private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
        private const val TOUCH_SCALE_FACTOR = 0.1f
    }
}