package org.jetbrains.skiko.native

import kotlinx.cinterop.pointed
import kotlinx.cinterop.useContents
import org.jetbrains.skiko.native.context.*
import org.jetbrains.skiko.native.redrawer.*
import org.jetbrains.skiko.skia.native.*

interface SkiaRenderer {
    fun onRender(canvas: SkCanvas, width: Int, height: Int, nanoTime: Long)
}

// TODO: this is exact copy of jvm counterpart. Commonize!
private class PictureHolder(val instance: SkPicture, val width: Int, val height: Int)

open class SkiaLayer(
    private val properties: SkiaLayerProperties = SkiaLayerProperties()
) : HardwareLayer() {
    var renderer: SkiaRenderer? = null

    //val clipComponents = mutableListOf<ClipRectangle>()

    internal var skiaState = createContextHandler(this)

    //@Volatile
    private var isDisposed = false

    private var redrawer: Redrawer? = null

    //@Volatile
    private var picture: PictureHolder? = null
    private val pictureRecorder = SkPictureRecorder()
    private val pictureLock = Any()

    override fun init() {
        super.init()
        redrawer = platformOperations.createRedrawer(this, properties)
        //redrawer?.syncSize()
        redrawer?.redrawImmediately()
    }

    override fun dispose() {
        //check(!isDisposed)
        //check(isEventDispatchThread())
        redrawer?.dispose()
        //picture?.instance?.close()
        //pictureRecorder.close()
        //isDisposed = true
        super.dispose()
    }
/*
    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        super.setBounds(x, y, width, height)
        redrawer?.syncSize()
        redrawer?.redrawImmediately()
    }
*/
    /*
    override fun paint(g: Graphics) {
        super.paint(g)
        redrawer?.syncSize()
        needRedraw()
    }
*/
    fun needRedraw() {
        //check(!isDisposed)
        //check(isEventDispatchThread())
        redrawer?.needRedraw()
    }

    //@Suppress("LeakingThis")
    //private val fpsCounter = defaultFPSCounter(this)

    override fun update(nanoTime: Long) {
        //check(!isDisposed)
        //check(isEventDispatchThread())

        //fpsCounter?.tick()

        val width = nsView.frame.useContents { size.width }
        val height = nsView.frame.useContents { size.height }

        val pictureWidth = (width * contentScale).coerceAtLeast(0.0)
        val pictureHeight = (height * contentScale).coerceAtLeast(0.0)

        val bounds = SkRect.MakeWH(pictureWidth.toFloat(), pictureHeight.toFloat())
        val canvas = pictureRecorder.beginRecording(bounds, null)!!

        // clipping
        //for (component in clipComponents) {
        //    canvas.clipRectBy(component)
        //}

        // TODO: get rid of .pointed?
        renderer?.onRender(canvas.pointed, pictureWidth.toInt(), pictureHeight.toInt(), nanoTime)

        // we can dispose layer during onRender
        //if (!isDisposed) {
            // synchronized(pictureLock) {


                // TODO:
                //picture?.instance?.close()
                val picture = pictureRecorder.finishRecordingAsPicture()!!
                this.picture = PictureHolder(picture, pictureWidth.toInt(), pictureHeight.toInt())
            // }
        //}
    }

    override fun draw() {
        //check(!isDisposed)
        skiaState.apply {
            //if (!initContext()) {
            //    fallbackToRaster()
            //    return
            //}
            initCanvas()
            clearCanvas()
            //synchronized(pictureLock) {
                val picture = picture
                if (picture != null) {
                    drawOnCanvas(picture.instance)
                }
            //}
            flush()
        }
    }
/*
    private fun SkCanvas.clipRectBy(rectangle: SkClipRectangle) {
        val dpi = contentScale
        clipRect(
            SkRect.MakeLTRB(
                rectangle.x * dpi,
                rectangle.y * dpi,
                (rectangle.x + rectangle.width) * dpi,
                (rectangle.y + rectangle.height) * dpi
            ),
            // TODO: SkClipOp is an enum. Should be working. But somehow the arg gets Int type.
            // SkClipOp::kDifference == 0.
            0,
            true
        )
    }

 */
/*
    private fun fallbackToRaster() {
        println("Falling back to software rendering...")
        redrawer?.dispose()
        skiaState = SoftwareContextHandler(this)
        redrawer = RasterRedrawer(this)
        needRedraw()
    }
    */
}
