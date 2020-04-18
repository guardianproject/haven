package org.havenapp.main.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.otaliastudios.cameraview.CameraView

/**
 * A subclass of [CameraView] which implements [DefaultLifecycleObserver]
 *
 * According to https://developer.android.com/reference/androidx/lifecycle/DefaultLifecycleObserver
 * [DefaultLifecycleObserver] should *always* be preferred over [androidx.lifecycle.LifecycleObserver]
 * if we use Java 8. [CameraView] library targets Java 7 hence this implementation aims to ignore
 * [androidx.lifecycle.OnLifecycleEvent] annotated methods in the super class and replace them with
 * the callbacks implemeted in this sub class
 */
class HavenCameraView : CameraView, DefaultLifecycleObserver {
    constructor(context: Context) : super(context)

    constructor(@NonNull context: Context, @Nullable attrs: AttributeSet) : super(context, attrs)

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        open()
    }

    override fun onPause(owner: LifecycleOwner) {
        close()
        super.onPause(owner)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        destroy()
        super.onDestroy(owner)
    }
}
