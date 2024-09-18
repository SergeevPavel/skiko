package org.jetbrains.skiko.menu

import javax.swing.Icon
import javax.swing.KeyStroke

class MacMenuItem : Disposable
{
    private var _icon: Icon? = null
    private var _accelerator : KeyStroke? = null
    private var _enabled = true
    private var _title: String? = null

    var nativePtr: Long = 0L
        private set

    @Suppress("MemberVisibilityCanBePrivate")
    var action: Runnable? = null

    @Suppress("MemberVisibilityCanBePrivate")
    var icon: Icon?
        get() = _icon
        set(value) {
            assert(nativePtr != 0L) {"MacMenuItem is disposed!"}

            _icon = value
            if (value == null) return

            nativeSetIcon(nativePtr, IconConverter.iconToByteArray(value)!!)
        }

    var accelerator : KeyStroke?
        get() = _accelerator
        set(value) {
            assert(nativePtr != 0L) {"MacMenuItem is disposed!"}

            _accelerator = value
            internalSetAccelerator(value)
        }

    var enabled : Boolean
        get() = _enabled
        set(value) {
            assert(nativePtr != 0L) {"MacMenuItem is disposed!"}

            _enabled = value
            nativeSetEnabled(nativePtr, value)
        }

    var title : String?
        get() = _title
        set(value) {
            assert(nativePtr != 0L) {"MacMenuItem is disposed!"}

            _title = value
            nativeSetTitle(nativePtr, value ?: "")
        }


    constructor(title: String, accelerator: KeyStroke? = null, icon: Icon? = null, action: Runnable? = null) {
        nativePtr = nativeCreate(title)

        this.title = title
        this.accelerator = accelerator
        this.icon = icon
        this.action = action
    }

    override fun dispose() {
        if (nativePtr == 0L) return

        nativeDispose(nativePtr)
        nativePtr = 0L
    }

    @Suppress("unused")
    fun handleAction() {
        // Called from AppKik
        action?.run()
    }

    private fun internalSetAccelerator(keyStroke: KeyStroke?)
    {
        // TODO: figure out why keyStroke.keyChar is equal to 0xFFFF
        // val keyChar = keyStroke?.keyChar ?: 0.toChar()
        val keyChar = 0.toChar()
        val keyCode = keyStroke?.keyCode ?: 0
        val modifiers = keyStroke?.modifiers ?: 0

        nativeSetAccelerator(
            nativePtr,
            keyChar,
            keyCode,
            modifiers
        )
    }

    // - Lifecycle
    private external fun nativeCreate(title: String): Long
    private external fun nativeDispose(nativePtr: Long)

    // - Operations
    private external fun nativeSetIcon(nativePtr: Long, data: ByteArray)
    private external fun nativeSetTitle(nativePtr: Long, title: String)
    private external fun nativeSetAccelerator(nativePtr: Long, keyChar: Char, keyCode: Int, modifiers: Int)
    private external fun nativeSetEnabled(nativePtr: Long, enabled: Boolean)
}