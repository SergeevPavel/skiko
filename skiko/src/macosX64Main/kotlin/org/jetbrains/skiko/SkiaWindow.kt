package org.jetbrains.skiko.native

import platform.AppKit.*
import platform.Cocoa.*
import platform.Foundation.*

open class SkiaWindow(
    properties: SkiaLayerProperties = SkiaLayerProperties()
) {

    val nsWindow = NSWindow(
        contentRect =  NSMakeRect(0.0, 0.0, 320.0, 200.0),
        styleMask = NSTitledWindowMask,
        backing =  NSBackingStoreBuffered,
        defer =  true)


    val layer = SkiaLayer(properties)

    init {
        nsWindow.contentView?.addSubview(layer.nsView)
    }
}
