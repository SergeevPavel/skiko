package org.jetbrains.skiko.menu

class MacMenuItemSeparator : Disposable {
    var nativePtr: Long = 0L
        private set

    init {
        nativePtr = nativeCreate()
    }

    override fun dispose() {
        if (nativePtr == 0L) return

        nativeDispose(nativePtr)
        nativePtr = 0L
    }

    private external fun nativeCreate(): Long
    private external fun nativeDispose(nativePtr: Long)
}