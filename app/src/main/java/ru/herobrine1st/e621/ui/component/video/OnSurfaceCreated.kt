package ru.herobrine1st.e621.ui.component.video

import android.view.SurfaceHolder

fun SurfaceHolder.onSurfaceCreated(callback: SurfaceHolder.() -> Unit) {
    addCallback(object: SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            holder.callback()
            this@onSurfaceCreated.removeCallback(this)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            this@onSurfaceCreated.removeCallback(this)
        }
    })
}