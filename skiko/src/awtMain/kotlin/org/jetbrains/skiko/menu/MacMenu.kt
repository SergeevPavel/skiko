package org.jetbrains.skiko.menu

open class MacMenu : Disposable {
    private var _enabled = true
    private var _title: String? = null
    private val _items = mutableListOf<Disposable>()

    @Suppress("MemberVisibilityCanBePrivate")
    var autoDisposeChildren = true

    var nativePtr: Long = 0L
        private set

    var enabled: Boolean
        get() = _enabled
        set(value) {
            assert(nativePtr != 0L) {"MacMenu is disposed!"}

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

    constructor() : this("")

    constructor(title: String) {
        nativePtr = nativeCreate(title)
        _title = title
    }

    override fun dispose() {
        if (nativePtr == 0L) return

        if (autoDisposeChildren) {
            _items.forEach { it.dispose() }
            _items.clear()
        }

        nativeDispose(nativePtr)
        nativePtr = 0L
    }

    fun add(menu: MacMenu) {
        assert(nativePtr != 0L) {"MacMenu is disposed!"}

        _items.add(menu)
        nativeAddMenu(nativePtr, menu.nativePtr)
    }

    fun add(menuItem: MacMenuItem) {
        assert(nativePtr != 0L) {"MacMenu is disposed!"}

        _items.add(menuItem)
        nativeAddMenuItem(nativePtr, menuItem.nativePtr)
    }

    fun add(separator: MacMenuItemSeparator) {
        assert(nativePtr != 0L) {"MacMenu is disposed!"}

        _items.add(separator)
        nativeAddMenuItem(nativePtr, separator.nativePtr)
    }

    fun remove(menu: MacMenu) {
        assert(nativePtr != 0L) {"MacMenu is disposed!"}

        _items.remove(menu)
        nativeRemoveMenu(nativePtr, menu.nativePtr)

        if (autoDisposeChildren) menu.dispose()
    }

    fun remove(menuItem: MacMenuItem) {
        assert(nativePtr != 0L) {"MacMenu is disposed!"}

        _items.remove(menuItem)
        nativeRemoveMenuItem(nativePtr, menuItem.nativePtr)

        if (autoDisposeChildren) menuItem.dispose()
    }

    fun remove(separator: MacMenuItemSeparator) {
        assert(nativePtr != 0L) {"MacMenu is disposed!"}

        _items.remove(separator)
        nativeRemoveMenuItem(nativePtr, separator.nativePtr)

        if (autoDisposeChildren) separator.dispose()
    }

    // - Lifecycle
    private external fun nativeCreate(label: String): Long
    private external fun nativeDispose(nativePtr: Long)

    // - Operations
    private external fun nativeSetEnabled(nativePtr: Long, enabled: Boolean)
    private external fun nativeSetTitle(nativePtr: Long, title: String)
    private external fun nativeAddMenuItem(nativePtr: Long, menuItemNativePtr: Long)
    private external fun nativeAddMenu(nativePtr: Long, menuNativePtr: Long)
    private external fun nativeRemoveMenu(nativePtr: Long, menuNativePtr: Long)
    private external fun nativeRemoveMenuItem(nativePtr: Long, menuItemNativePtr: Long)
}