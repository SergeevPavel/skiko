package SkiaAwtSample

import kotlinx.coroutines.*
import org.jetbrains.skia.PixelGeometry
import org.jetbrains.skiko.*
import org.jetbrains.skiko.menu.*
import java.awt.Color
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.*
import java.awt.RenderingHints
import javax.swing.*
import javax.swing.JOptionPane.*
import javax.swing.SwingUtilities.invokeLater
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    // Don't use AWT macOS menu implementation
    System.setProperty("skiko.rendering.useScreenMenuBar", "false")

    setupSkikoLoggerFactory { DefaultConsoleLogger.fromLevel(System.getProperty("skiko.log.level", "INFO")) }
    val windows = parseArgs(args)
    repeat(windows) {
        when (System.getProperty("skiko.swing.interop")) {
            "true" -> swingSkia()
            else -> createWindow("window $it", windows == 1)
        }
    }
}

fun createWindow(title: String, exitOnClose: Boolean) = SwingUtilities.invokeLater {
    val renderingHints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints") as Map<Any, Any>
    val pixelGeometry = when (renderingHints[RenderingHints.KEY_TEXT_ANTIALIASING]) {
        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB -> PixelGeometry.RGB_H
        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR -> PixelGeometry.BGR_H
        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB -> PixelGeometry.RGB_V
        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR -> PixelGeometry.BGR_V
        else -> PixelGeometry.UNKNOWN
    }
    val skiaLayer = SkiaLayer(pixelGeometry = pixelGeometry)
    val clocks = ClocksAwt(skiaLayer)

    val window = JFrame(title)
    window.defaultCloseOperation =
        if (exitOnClose) WindowConstants.EXIT_ON_CLOSE else WindowConstants.DISPOSE_ON_CLOSE
    window.background = Color.GREEN
    window.contentPane.add(skiaLayer)

    // Create menu.
    val menuBar = createMenuBar()

    skiaLayer.onStateChanged(SkiaLayer.PropertyKind.Renderer) { layer ->
        println("Changed renderer for $layer: new value is ${layer.renderApi}")
    }

    skiaLayer.renderDelegate = SkiaLayerRenderDelegate(skiaLayer, clocks)
    skiaLayer.addMouseMotionListener(clocks)

    // Window transparency
    if (System.getProperty("skiko.transparency") == "true") {
        window.isUndecorated = true

        /**
         * There is a hack inside skiko OpenGL and Software redrawers for Windows that makes current
         * window transparent without setting `background` to JDK's window. It's done by getting native
         * component parent and calling `DwmEnableBlurBehindWindow`.
         *
         * FIXME: Make OpenGL work inside transparent window (background == Color(0, 0, 0, 0)) without this hack.
         *
         * See `enableTransparentWindow` (skiko/src/awtMain/cpp/windows/window_util.cc)
         */
        if (hostOs != OS.Windows || skiaLayer.renderApi == GraphicsApi.DIRECT3D) {
            window.background = Color(0, 0, 0, 0)
        }
        skiaLayer.transparency = true

        /*
         * Windows makes clicks on transparent pixels fall through, but it doesn't work
         * with GPU accelerated rendering since this check requires having access to pixels from CPU.
         *
         * JVM doesn't allow override this behaviour with low-level windows methods, so hack this in this way.
         * Based on tests, it doesn't affect resulting pixel color.
         */
        if (hostOs == OS.Windows) {
            val contentPane = window.contentPane as JComponent
            contentPane.background = Color(0, 0, 0, 1)
            contentPane.isOpaque = true
        }
    } else {
        skiaLayer.background = Color.LIGHT_GRAY
    }

    // MANDATORY: set window preferred size before calling pack()
    window.preferredSize = Dimension(800, 600)
    window.pack()
    skiaLayer.disableTitleBar(64f)
    window.pack()
    skiaLayer.paint(window.graphics)
    window.isVisible = true
}

private fun parseArgs(args: Array<String>): Int {
    var windows = 1
    for(arg in args) {
        try {
            windows = arg.toInt()
            break      
        }
        catch(e: NumberFormatException) {
            println("The passed argument:($arg) is not a integer number!")
        }
    }
    return windows
}

private fun createMenuBar(): MacMenuBar
{
    val menuBar = MacMenuBar()

    // [App]
    val appMenu = MacMenu("App")
    appMenu.add(MacMenuItem(
        "About...",
        action = { invokeLater {
            showMessageDialog(null, "Mac Menu test App", "About", INFORMATION_MESSAGE)
        }}
    ))

    appMenu.add(MacMenuItem(
        "Destroy main menu",
        action = { invokeLater { menuBar.dispose() }}
    ))

    appMenu.add(MacMenuItemSeparator())

    appMenu.add(MacMenuItem(
        "Quit",
        accelerator = KeyStroke.getKeyStroke("meta pressed Q"),
        action = { invokeLater { java.lang.System.exit(0) }}
    ))

    // [Edit]
    val editMenu = MacMenu("Edit")

    val editMenuCopyMenuItem = MacMenuItem(
        "Copy",
        accelerator = KeyStroke.getKeyStroke("meta pressed C"),
        action = { invokeLater {
            showMessageDialog(null, "Copy!", "Message", INFORMATION_MESSAGE)
        }}
    )

    val editMenuPasteMenuItem = MacMenuItem(
        "Paste",
        accelerator = KeyStroke.getKeyStroke("meta pressed V")
    )
    editMenuPasteMenuItem.enabled = false

    val editMenuRemoveMeMenuItem = MacMenuItem("Remove me")
    editMenuRemoveMeMenuItem.action = Runnable { invokeLater {
        editMenu.remove(editMenuRemoveMeMenuItem)
    }}

    val editMenuSubmenu = MacMenu("Submenu")

    val editMenuSubmenuRemoveSubmenu = MacMenuItem(
        "Remove this Submenu",
        action = { invokeLater { editMenu.remove(editMenuSubmenu) }}
    )

    val editMenuSubmenuDisableSubmenu = MacMenuItem(
        "Disable this Submenu",
        action = { invokeLater { editMenuSubmenu.enabled = false }}
    )

    editMenuSubmenu.add(editMenuSubmenuRemoveSubmenu)
    editMenuSubmenu.add(MacMenuItemSeparator())
    editMenuSubmenu.add(editMenuSubmenuDisableSubmenu)

    editMenu.add(editMenuCopyMenuItem)
    editMenu.add(editMenuPasteMenuItem)
    editMenu.add(editMenuRemoveMeMenuItem)
    editMenu.add(editMenuSubmenu)

    // [View]
    val viewMenu = MacMenu("View")

    val mainMenu = MacMenu("Main Menu")
    mainMenu.add(appMenu)
    mainMenu.add(editMenu)
    mainMenu.add(viewMenu)

    menuBar.mainMenu = mainMenu

    return menuBar
}