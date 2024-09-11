package org.jetbrains.skiko.menu

class MacMenuBar : Disposable {
    private var _mainMenu: MacMenu? = null

    @Suppress("MemberVisibilityCanBePrivate")
    var mainMenu: MacMenu?
        get() = _mainMenu
        set(value) {
            if (value == _mainMenu) return

            _mainMenu?.dispose()
            _mainMenu = value

            if (value == null) {
                nativeSetMainMenu(0L)
                return
            }

            nativeSetMainMenu(value.nativePtr)
        }

    override fun dispose() {
        mainMenu = null
    }

    private external fun nativeSetMainMenu(nativeMenu: Long)
}