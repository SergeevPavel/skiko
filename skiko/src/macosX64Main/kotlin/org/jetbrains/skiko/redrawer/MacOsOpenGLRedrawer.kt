package org.jetbrains.skiko.native.redrawer

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.jetbrains.skiko.native.*
import platform.CoreFoundation.CFTimeInterval
import platform.CoreGraphics.CGRectMake
import platform.CoreVideo.CVTimeStamp
import platform.OpenGL3.GL_COLOR_BUFFER_BIT
import platform.OpenGL3.glClear
import platform.OpenGL3.glClearColor
import platform.OpenGLCommon.CGLContextObj
import platform.OpenGLCommon.CGLPixelFormatObj
import platform.OpenGLCommon.CGLSetCurrentContext
import platform.QuartzCore.CAOpenGLLayer
import platform.QuartzCore.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.system.getTimeNanos

// Current implementation is fragile (it works in all tested cases, but we can't test everything)
//
// We should investigate can we implement our own CAOpenGLLayer, without its restrictions.
// (see, for example https://github.com/gnustep/libs-gui/blob/master/Source/NSOpenGLView.m)
//
// P.S. MacOsOpenGLRedrawer will not be used by default in the future, because we will support Metal.

internal class MacOsOpenGLRedrawer(
    private val layer: HardwareLayer,
    private val properties: SkiaLayerProperties
) : Redrawer {
    private val containerLayer = layer
    private val drawLock = Any()
    private var isDisposed = false

    private val drawLayer = object : MacosGLLayer(containerLayer, setNeedsDisplayOnBoundsChange = true) {
        override fun draw() = //synchronized(drawLock) {
            //if (!isDisposed) {
                layer.draw()
            //}
        //}

        suspend fun display() = display(::setNeedsDisplay)
    }

    // use a separate layer for vsync, because with single layer we cannot asynchronously update layer
    // `update` is suspend, and runBlocking(Dispatchers.Swing) causes dead lock with AppKit Thread.
    // AWT has a method to avoid dead locks but it is internal (sun.lwawt.macosx.LWCToolkit.invokeAndWait)
    private val vsyncLayer = object : MacosGLLayer(containerLayer, setNeedsDisplayOnBoundsChange = false) {
        //@Volatile
        private var canDraw = false

        init {
            setFrame(0, 0, 1, 1) // if frame has zero size then it will be not drawn at all
        }

        override fun draw() {
            // Clear layer with transparent color, so it will be not pink color.
            glClearColor(0f, 0f, 0f, 0f)
            glClear(GL_COLOR_BUFFER_BIT)
        }

        override fun canDraw(): Boolean {
            val canDraw = canDraw
            if (!canDraw) {
                isAsynchronous = false // stop asynchronous mode so we don't waste CPU cycles
            }
            return canDraw
        }

        private fun requestAsyncDisplay() {
            // Use asynchronous mode instead of just setNeedsDisplay,
            // so Core Animation will wait for the next frame in vsync signal
            //
            // Asynchronous mode means that Core Animation will automatically
            // call canDraw/draw every vsync signal (~16.7ms on 60Hz monitor)
            //
            // Similar is implemented in Chromium:
            // https://chromium.googlesource.com/chromium/chromium/+/0489078bf98350b00876070cf2fdce230905f47e/content/browser/renderer_host/compositing_iosurface_layer_mac.mm#57
            if (!isAsynchronous) {
                isAsynchronous = true
                setNeedsDisplay()
            }
        }

        suspend fun sync() {
            canDraw = true
            display(::requestAsyncDisplay)
            canDraw = false
        }
    }

    private val frameDispatcher = FrameDispatcher(Dispatchers.Swing) {
        //synchronized(drawLock) {
            layer.update(getTimeNanos())
        //}
        if (properties.isVsyncEnabled) {
            drawLayer.setNeedsDisplay()
            vsyncLayer.sync()
        } else {
            error("Drawing without vsync isn't supported on macOs with OpenGL")
        }
    }

    override fun dispose() = synchronized(drawLock) {
        frameDispatcher.cancel()
        vsyncLayer.dispose()
        drawLayer.dispose()
        isDisposed = true
    }

    override fun syncSize() {
        // TODO: What do we really do here?
        drawLayer.setFrame(
            layer.nsView.frame.useContents { origin.x }.toInt(),
            layer.nsView.frame.useContents { origin.y }.toInt(),
            layer.nsView.frame.useContents { size.width }.toInt().coerceAtLeast(0),
            layer.nsView.frame.useContents { size.height }.toInt().coerceAtLeast(0)
        )
        /*
        val globalPosition = convertPoint(layer, layer.x, layer.y, getRootPane(layer))
        setContentScale(containerLayerPtr, layer.contentScale)
        setContentScale(drawLayer.ptr, layer.contentScale)
        drawLayer.setFrame(
            globalPosition.x,
            globalPosition.y,
            layer.width.coerceAtLeast(0),
            layer.height.coerceAtLeast(0)
        )
        */
    }

    override fun needRedraw() {
        frameDispatcher.scheduleFrame()
    }

    override fun redrawImmediately() {
        layer.update(getTimeNanos())

        // macOs will call 'draw' itself because of 'setNeedsDisplayOnBoundsChange=true'.
        // But we schedule new frame after vsync anyway.
        // Because 'redrawImmediately' can be called after 'draw',
        // and we need at least one 'draw' after 'redrawImmediately'.
        //
        // We don't use setNeedsDisplay, because frequent calls of it are unreliable.
        // 'setNeedsDisplayOnBoundsChange=true' with combination of 'scheduleFrame' is enough
        // to not see the white bars on resize.
        frameDispatcher.scheduleFrame()
    }
}

class NativeMacosGLLayer {
    val caOpenGLLayer = CAOpenGLLayer().also {
        it.removeAllAnimations()
        it.setAutoresizingMask(kCALayerWidthSizable or kCALayerHeightSizable )
    }

    fun canDrawInCGLContext(
        ctx: CGLContextObj?,
        pixelFormat: CGLPixelFormatObj?,
        forLayerTime: CFTimeInterval,
        displayTime: CPointer<CVTimeStamp>?
    ): Boolean {
        return caOpenGLLayer.canDrawInCGLContext(ctx, pixelFormat, forLayerTime, displayTime)
    }

    fun drawInCGLContext(
        ctx: CGLContextObj?,
        pixelFormat: CGLPixelFormatObj?,
        forLayerTime: CFTimeInterval,
        displayTime: CPointer<CVTimeStamp>?
    ) {
        CGLSetCurrentContext(ctx);
        caOpenGLLayer.drawInCGLContext(ctx, pixelFormat,forLayerTime, displayTime)
    }

    companion object {
        fun initGlLayer(caLayer: CALayer, setNeedsDisplayOnBoundsChange: Boolean): NativeMacosGLLayer {
            val glLayer = NativeMacosGLLayer();
            glLayer.caOpenGLLayer.setNeedsDisplayOnBoundsChange(setNeedsDisplayOnBoundsChange)
            caLayer.addSublayer(glLayer.caOpenGLLayer)
            return glLayer
        }

        fun setFrame(
            container: CALayer,
            glLayer: CAOpenGLLayer,
            x: Float, y: Float, width: Float, height: Float
        ) {

            val newY = container.frame.useContents { size.height } - y - height

            CATransaction.begin()
            CATransaction.setDisableActions(true)
            glLayer.frame = CGRectMake(x.toDouble(), newY, width.toDouble(), height.toDouble())
            CATransaction.commit()
        }

        fun disposeGLLayer(glLayer: CAOpenGLLayer) {
            glLayer.removeFromSuperlayer()
            // glLayer.release()
        }

        fun isAsynchronous(glLayer: CAOpenGLLayer): Boolean {
            return glLayer.isAsynchronous();
        }

        fun setAsynchronous(glLayer: CAOpenGLLayer, isAsynchronous: Boolean) {
            glLayer.setAsynchronous(isAsynchronous)
        }

        fun setNeedsDisplayOnMainThread(glLayer: CAOpenGLLayer)
        {
            dispatch_async(dispatch_get_main_queue(), {
                glLayer.setNeedsDisplay()
            })
        }
    }
}

private abstract class MacosGLLayer(layer: HardwareLayer, setNeedsDisplayOnBoundsChange: Boolean) {
    @Suppress("LeakingThis")
    val container = layer.nsView.layer ?: error("No layer for NSView")
    val ptr = NativeMacosGLLayer.initGlLayer(container, setNeedsDisplayOnBoundsChange)

    private val display = Task()

    fun setFrame(x: Int, y: Int, width: Int, height: Int) {
        NativeMacosGLLayer.setFrame(container, ptr.caOpenGLLayer, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
    }

    // Called in AWT Thread
    open fun dispose() = NativeMacosGLLayer.disposeGLLayer(ptr.caOpenGLLayer)

    var isAsynchronous: Boolean
        get() = NativeMacosGLLayer.isAsynchronous(ptr.caOpenGLLayer)
        set(value) = NativeMacosGLLayer.setAsynchronous(ptr.caOpenGLLayer, value)

    /**
     * Schedule next [draw] as soon as possible (not waiting for vsync)
     *
     * WARNING!!!
     *
     * CAOpenGLLayer will not call [draw] if we call [setNeedsDisplay] too often.
     *
     * Experimentally we found out that after 15 draw's between two vsync's (900 FPS on 60 Hz display) will cause
     * setNeedsDisplay to not schedule the next draw at all.
     *
     * Only after the next vsync, [setNeedsDisplay] will be working again.
     */
    fun setNeedsDisplay() {
        NativeMacosGLLayer.setNeedsDisplayOnMainThread(ptr.caOpenGLLayer)
    }

    protected suspend fun display(
        startDisplay: () -> Unit
    ) = display.runAndAwait {
        startDisplay()
    }

    // Called in AppKit Thread
    protected open fun canDraw() = true

    // Called in AppKit Thread
    @Suppress("unused") // called from native code
    private fun performDraw() {
        try {
            draw()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        display.finish()
    }

    // Called in AppKit Thread
    protected abstract fun draw()

}