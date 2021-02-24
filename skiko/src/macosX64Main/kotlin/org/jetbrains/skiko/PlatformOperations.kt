package org.jetbrains.skiko.native

import org.jetbrains.skiko.native.*
import org.jetbrains.skiko.native.SkikoProperties.renderApi
import org.jetbrains.skiko.native.redrawer.*

internal interface PlatformOperations {
    /*
    fun isFullscreen(component: Component): Boolean
    fun setFullscreen(component: Component, value: Boolean)
    fun getDpiScale(component: Component): Float
     */
    fun createRedrawer(layer: HardwareLayer, properties: SkiaLayerProperties): Redrawer
}

internal val platformOperations: PlatformOperations by lazy {
    //when (hostOs) {
        /*OS.MacOS -> */object: PlatformOperations {
    /*
                override fun isFullscreen(component: Component): Boolean {
                    return osxIsFullscreenNative(component)
                }

                override fun setFullscreen(component: Component, value: Boolean) {
                    osxSetFullscreenNative(component, value)
                }

                override fun getDpiScale(component: Component): Float {
                    return component.graphicsConfiguration.defaultTransform.scaleX.toFloat()
                }
*/
                override fun createRedrawer(layer: HardwareLayer, properties: SkiaLayerProperties) = when(renderApi) {
                    GraphicsApi.SOFTWARE -> error("No software rendering for native yet")//RasterRedrawer(layer)
                    else -> MacOsOpenGLRedrawer(layer, properties)
                }
        }
        /*
        OS.Windows -> {
            object: PlatformOperations {
                override fun isFullscreen(component: Component): Boolean {
                    val window = SwingUtilities.getRoot(component) as Window
                    val device = window.graphicsConfiguration.device
                    return device.getFullScreenWindow() == window
                }

                override fun setFullscreen(component: Component, value: Boolean) {
                    val window = SwingUtilities.getRoot(component) as Window
                    val device = window.graphicsConfiguration.device
                    device.setFullScreenWindow(if (value) window else null)
                }

                override fun getDpiScale(component: Component): Float {
                    return component.graphicsConfiguration.defaultTransform.scaleX.toFloat()
                }

                override fun createRedrawer(layer: HardwareLayer, properties: SkiaLayerProperties) = when(renderApi) {
                    GraphicsApi.SOFTWARE -> RasterRedrawer(layer)
                    else -> WindowsOpenGLRedrawer(layer, properties)
                }
            }
        }
        OS.Linux -> {
            object: PlatformOperations {
                override fun isFullscreen(component: Component): Boolean {
                    val window = SwingUtilities.getRoot(component) as Window
                    val device = window.graphicsConfiguration.device
                    return device.getFullScreenWindow() == window
                }

                override fun setFullscreen(component: Component, value: Boolean) {
                    val window = SwingUtilities.getRoot(component) as Window
                    val device = window.graphicsConfiguration.device
                    device.setFullScreenWindow(if (value) window else null)
                }

                override fun getDpiScale(component: Component): Float {
                    return component.graphicsConfiguration.defaultTransform.scaleX.toFloat()
                    // TODO doesn't work well because java doesn't scale windows (content has offset with 200% scale)
                    //
                    // Two solutions:
                    // 1. dynamically change sun.java2d.uiScale (it is global property, so we have to be careful) and update all windows
                    //
                    // 2. apply contentScale manually to all windows
                    // (it is not good, because on different platform windows will have different size.
                    // Maybe we will apply contentScale manually on all platforms?)

                    // see also comment for HardwareLayer.checkContentScale

//                    return component.useDrawingSurfacePlatformInfo(::linuxGetDpiScaleNative)
                }

                override fun createRedrawer(layer: HardwareLayer, properties: SkiaLayerProperties) = when(renderApi) {
                    GraphicsApi.SOFTWARE -> RasterRedrawer(layer)
                    else -> LinuxOpenGLRedrawer(layer, properties)
                }
            }
        }
    }
*/
}