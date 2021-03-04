package org.jetbrains.skiko.native

import kotlinx.cinterop.readValue
import platform.Cocoa.*
import platform.AppKit.*
import platform.Foundation.*
import platform.CoreGraphics.*

abstract class HardwareLayer {

    val nsView = NSView(NSMakeRect(0.0, 0.0, 640.0, 480.0))

    // getDpiScale is expensive operation on some platforms, so we cache it
    private var _contentScale: Float? = null
    private var isInit = false


    init {

        // TODO: what's the proper way to do it for native Macos?

        //@Suppress("LeakingThis")
        //addHierarchyListener {
        //    if (it.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
        //        checkIsShowing()
        //    }
        //}
    }

    /*private*/ fun checkIsShowing() {
        if (!isInit && !nsView.hiddenOrHasHiddenAncestor) {
            // _contentScale = getDpiScale()
            _contentScale = 1.0f // TODO: what's the proper way here?
            init()
            isInit = true
        }
    }


    /*protected*/ open fun init() {
 //       useDrawingSurfacePlatformInfo(::nativeInit)
    }

    protected open fun nativeInit(platformInfo: Long) {

    }

    open fun dispose() {

    }

    protected open fun contentScaleChanged() = Unit

    //override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
    //
    //    nsView.bounds = NSMakeRect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
    //}

    //override fun paint(g: Graphics) {
    //    checkContentScale()
    //}

    // TODO checkContentScale is called before init. it is ok, but when we fix getDpiScale on Linux we should check [isInit]
    private fun checkContentScale() {
    //    val contentScale = getDpiScale()
    //    if (contentScale != _contentScale) {
    //        _contentScale = contentScale
    //        contentScaleChanged()
    //    }
    }
/*
    private fun getDpiScale(): Float {
        val scale = platformOperations.getDpiScale(this)
        check(scale > 0) { "HardwareLayer.contentScale isn't positive: $contentScale"}
        return scale
    }
*/
    // Should be called in Swing thread
    internal abstract fun update(nanoTime: Long)

    // Should be called in the OpenGL thread, and only once after update
    internal abstract fun draw()

//    val windowHandle: Long
//        get() = useDrawingSurfacePlatformInfo(::getWindowHandle)

    val contentScale: Float
        get() = _contentScale!!
/*
    var fullscreen: Boolean
        get() = platformOperations.isFullscreen(this)
        set(value) = platformOperations.setFullscreen(this, value)
*/
//    private external fun getWindowHandle(platformInfo: Long): Long
}