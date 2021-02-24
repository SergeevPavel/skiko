package org.jetbrains.skiko.native.context

import kotlinx.cinterop.*
import org.jetbrains.skiko.skia.native.*
import org.jetbrains.skiko.native.*
import platform.OpenGL.GL_DRAW_FRAMEBUFFER_BINDING
import platform.OpenGL.GL_RGBA8
import platform.OpenGL.glGetIntegerv
import platform.OpenGLCommon.GLenum

internal class OpenGLContextHandler(layer: HardwareLayer) : ContextHandler(layer) {
    override fun initContext(): Boolean {
        try {
            if (context == null) {
                context = GrDirectContext.MakeGL()
            }
        } catch (e: Exception) {
            println("Failed to create Skia OpenGL context!")
            return false
        }
        return true
    }

    @ExperimentalUnsignedTypes
    private fun openglGetIntegerv(pname: GLenum): UInt {
        var result: UInt = 0U
        memScoped {
            val data = alloc<IntVar>()
            glGetIntegerv(pname, data.ptr);
            result = data.value.toUInt();
        }
        return result
    }

    @ExperimentalUnsignedTypes
    override fun initCanvas() {
        dispose()

        val scale = layer.contentScale
        val w = (layer.nsView.frame.useContents { size.width } * scale).toInt().coerceAtLeast(0)
        val h = (layer.nsView.frame.useContents { size.height } * scale).toInt().coerceAtLeast(0)

        val fbId = openglGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING.toUInt())
        val glInfo: GrGLFramebufferInfo = GrGLFramebufferInfo().also { // TODO: Skia C++ interop: need a constructor here.
            it.fFBOID = fbId
            it.fFormat = kRGBA8.toUInt()
        }
        renderTarget = GrBackendRenderTarget(w, h, 0, 0, glInfo.ptr)

        surface = SkSurface.MakeFromBackendRenderTarget(
            // TODO: C++ interop knows nothing about inheritance.
            context!!.ptr.reinterpret<GrRecordingContext>(),
            renderTarget!!.ptr,
            GrSurfaceOrigin.kBottomLeft_GrSurfaceOrigin,
            //SurfaceColorFormat.RGBA_8888,
            colorType = kBGRA_8888_SkColorType,
            colorSpace = SkColorSpace.MakeSRGB() // TODO: Skia C++ interop: need passing sk_sp.
        )

        canvas = surface!!.getCanvas()?.pointed
    }
}
