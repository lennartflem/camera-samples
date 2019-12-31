package com.android.example.cameraxbasic.utils

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import androidx.camera.core.Preview
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

class PreviewSurfaceProvider: Preview.PreviewSurfaceProvider {

     private lateinit var mSurfaceTexture: SurfaceTexture
     private lateinit var mSurface: Surface

     @SuppressLint("RestrictedApi")
     override fun provideSurface(
             resolution: Size, surfaceReleaseFuture: ListenableFuture<Void>
     ): ListenableFuture<Surface> {

        // Create the SurfaceTexture with the required resolution
        mSurfaceTexture = SurfaceTexture(0)
        mSurfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)
        mSurfaceTexture.detachFromGLContext()

        mSurface = Surface(mSurfaceTexture)

         // Once surfaceReleaseFuture completes, the Surface and SurfaceTexture
         // are no longer used by the camera hence safe to close.
         surfaceReleaseFuture.addListener(
             Runnable {
                 mSurface.release()
                 mSurfaceTexture.release()
             }, Executors.newSingleThreadExecutor()
         )

         // Return the Surface back in a ListenableFuture
         return Futures.immediateFuture(mSurface)
     }
}