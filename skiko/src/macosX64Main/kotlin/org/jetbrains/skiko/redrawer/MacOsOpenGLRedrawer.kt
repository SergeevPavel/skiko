package org.jetbrains.skiko.native.redrawer

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.useContents
//import kotlinx.coroutines.Dispatchers
import org.jetbrains.skiko.native.*
import platform.CoreFoundation.CFTimeInterval
import platform.CoreGraphics.CGRectMake
import platform.CoreVideo.CVTimeStamp
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
    //private val drawLock = Any()
    private var isDisposed = false

    private val drawLayer = MacosGLLayer(layer, setNeedsDisplayOnBoundsChange = true)
/*
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
*/
    override fun dispose() { // = synchronized(drawLock) {
        //frameDispatcher.cancel()
        //vsyncLayer.dispose()
        drawLayer.dispose()
        //isDisposed = true
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
        //frameDispatcher.scheduleFrame()
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

        //frameDispatcher.scheduleFrame()
    }
}

class MacosGLLayer(val layer: HardwareLayer, setNeedsDisplayOnBoundsChange: Boolean) : CAOpenGLLayer() {

    val container = layer.nsView

    init {
        this.setNeedsDisplayOnBoundsChange(setNeedsDisplayOnBoundsChange)
        this.removeAllAnimations()
        this.setAutoresizingMask(kCALayerWidthSizable or kCALayerHeightSizable )
        container.layer = this
        container.wantsLayer = true
    }

    //private val display = Task()

    fun draw()  { //= synchronized(drawLock) {
        println("MacosGLLayer::draw")
        //if (!isDisposed) {
            layer.update(getTimeNanos())
            layer.draw()
        //}
    }

    fun setFrame(x: Int, y: Int, width: Int, height: Int) {
        val newY = container.frame.useContents { size.height } - y - height

        CATransaction.begin()
        CATransaction.setDisableActions(true)
        this.frame = CGRectMake(x.toDouble(), newY, width.toDouble(), height.toDouble())
        CATransaction.commit()
    }

    // Called in AWT Thread
    fun dispose() {
        this.removeFromSuperlayer()
        // TODO: anything else to dispose the layer?
    }

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
    //fun setNeedsDisplay() {
    //
    //}

    //protected suspend fun display(
    //    startDisplay: () -> Unit
    //) = display.runAndAwait {
    //    startDisplay()
    //}

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
        //display.finish()
    }

    override fun canDrawInCGLContext(
        ctx: CGLContextObj?,
        pixelFormat: CGLPixelFormatObj?,
        forLayerTime: CFTimeInterval,
        displayTime: CPointer<CVTimeStamp>?
    ): Boolean {
        return canDraw()
    }

    override fun drawInCGLContext(
        ctx: CGLContextObj?,
        pixelFormat: CGLPixelFormatObj?,
        forLayerTime: CFTimeInterval,
        displayTime: CPointer<CVTimeStamp>?
    ) {
        CGLSetCurrentContext(ctx);
        println("MacosGLLayer::drawInCGLContext")
        println("DRAWING")

        performDraw()

        //context.flush() // TODO: I thought the below should call context.flush().
        super.drawInCGLContext(ctx, pixelFormat,forLayerTime, displayTime)
    }

    fun setNeedsDisplayOnMainThread(glLayer: CAOpenGLLayer) {
        dispatch_async(dispatch_get_main_queue(), {
            this.setNeedsDisplay()
        })
    }
}
