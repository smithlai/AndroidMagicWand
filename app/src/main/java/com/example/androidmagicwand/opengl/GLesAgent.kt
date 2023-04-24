package com.example.androidmagicwand.opengl

import android.app.ActivityManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class GLesAgent{

    public constructor(){
    }
    var context: android.content.Context? = null
    var mGLTextureView: myGLTextureView? = null
    public fun setup(context:android.content.Context, mGLglTextureView: myGLTextureView?):Boolean{
        this.context = context
        this.mGLTextureView = mGLglTextureView
        if ( mGLglTextureView != null && detectOpenGLES30()) {
            //so we know it a opengl 3.0 and use our extended GLTextureView
            mGLTextureView!!.setRenderer(myRenderer(context))
        } else {
            // This is where you could create an OpenGL ES 2.0 and/or 1.x compatible
            // renderer if you wanted to support both ES 1 and ES 2.
            Log.e("openglcube", "OpenGL ES 3.0 not supported on device.  Exiting...")
            return false
        }
        return true
    }

    fun detectOpenGLES30(): Boolean {
        context?.let {
            val am = it.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager
            val info = am.deviceConfigurationInfo
            Log.e("AAAA", "info.reqGlEsVersion: " + Integer.toHexString(info.reqGlEsVersion))
            return info.reqGlEsVersion >= 0x30000
        }
        return false
    }
    fun onResume(){
        mGLTextureView?.let {
            if (!it.isRunning) {
                it.setRenderer(myRenderer(context));
            }

            //start up the animation.
            it.isPaused = false
        }
    }

    fun onPause(){
        //stop the animation.
        mGLTextureView?.isPaused = true
    }

}