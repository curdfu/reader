package com.htmake.reader

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.SwingUtilities
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object TrayApplication {
    private var serviceProcess: Process? = null
    private var trayIcon: TrayIcon? = null
    private var isRunning = false
    private var port = 8080

    @JvmStatic
    fun main(args: Array<String>) {
        args.forEach { arg ->
            if (arg.startsWith("--port=")) {
                parsePort(arg.substringAfter("="))?.let { port = it }
            }
        }
        if (!SystemTray.isSupported()) { exitProcess(1) }
        SwingUtilities.invokeLater { createAndShowTray() }
    }

    private fun createAndShowTray() {
        val popup = PopupMenu()
        val statusItem = MenuItem(statusText()).apply { isEnabled = false }
        val startItem = MenuItem("Start Service").apply { addActionListener { startService() } }
        val stopItem = MenuItem("Stop Service").apply { addActionListener { stopService() } }
        val openItem = MenuItem("Open Reader").apply { addActionListener { openReader() } }
        val exitItem = MenuItem("Exit").apply { addActionListener { exitApp() } }

        popup.add(statusItem)
        popup.addSeparator()
        popup.add(startItem)
        popup.add(stopItem)
        popup.add(openItem)
        popup.addSeparator()
        popup.add(exitItem)

        trayIcon = TrayIcon(createIcon(Color(100, 100, 100)), "Reader").apply {
            isImageAutoSize = true
            setPopupMenu(popup)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        openReader()
                    }
                }
            })
        }
        try { SystemTray.getSystemTray().add(trayIcon) } catch (e: AWTException) { exitProcess(1) }

        thread(isDaemon = true) {
            while (true) {
                Thread.sleep(2000)
                checkServiceStatus()
                SwingUtilities.invokeLater {
                    statusItem.label = statusText()
                    trayIcon?.image = if (isRunning) createIcon(Color(76, 175, 80)) else createIcon(Color(100, 100, 100))
                }
            }
        }
        thread(isDaemon = true) { Thread.sleep(500); startService() }
    }

    private fun statusText() = if (isRunning) "Status: Running ($port)" else "Status: Stopped ($port)"

    private fun parsePort(value: String): Int? {
        val parsed = value.trim().toIntOrNull() ?: return null
        return parsed.takeIf { it in 1..65535 }
    }

    private fun openReader() {
        try {
            Desktop.getDesktop().browse(java.net.URI("http://localhost:$port"))
        } catch (e: Exception) {
            trayIcon?.displayMessage("Reader", "Open failed: ${e.message}", TrayIcon.MessageType.ERROR)
        }
    }


    private fun createIcon(color: Color): Image {
        val s = 64
        val img = BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        val accent = color
        val shadow = Color(0, 0, 0, 56)
        val coverDark = Color(
            (accent.red * 0.62).toInt().coerceIn(0, 255),
            (accent.green * 0.62).toInt().coerceIn(0, 255),
            (accent.blue * 0.62).toInt().coerceIn(0, 255)
        )
        val coverLight = Color(
            (accent.red + 44).coerceAtMost(255),
            (accent.green + 44).coerceAtMost(255),
            (accent.blue + 44).coerceAtMost(255)
        )

        g.color = shadow
        g.fillRoundRect(10, 12, 45, 43, 14, 14)

        g.paint = GradientPaint(8f, 8f, coverLight, 54f, 56f, coverDark)
        g.fillRoundRect(7, 6, 47, 48, 14, 14)

        g.color = Color(255, 255, 255, 60)
        g.drawRoundRect(8, 7, 45, 46, 13, 13)

        g.color = Color.WHITE
        g.fillRoundRect(19, 14, 27, 38, 6, 6)
        g.color = Color(232, 238, 244)
        g.drawLine(24, 21, 41, 21)
        g.drawLine(24, 28, 41, 28)
        g.drawLine(24, 35, 38, 35)
        g.drawLine(24, 42, 36, 42)

        g.color = accent
        g.fillRoundRect(15, 15, 8, 36, 4, 4)
        g.color = Color(255, 255, 255, 120)
        g.drawLine(17, 18, 17, 47)

        g.dispose()
        return img
    }

    private fun startService() {
        if (isRunning) return
        val jar = findReaderJar() ?: run {
            trayIcon?.displayMessage("Reader", "Cannot find reader.jar", TrayIcon.MessageType.ERROR)
            return
        }
        try {
            val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
            val workDir = File(System.getProperty("user.dir"))
            val proc = ProcessBuilder(
                javaBin, "-jar", jar.absolutePath, "--reader.server.port=$port"
            )
            proc.redirectErrorStream(true)
            proc.directory(workDir)
            serviceProcess = proc.start()

            // Drain stdout to prevent pipe buffer deadlock
            thread(isDaemon = true) {
                try {
                    serviceProcess!!.inputStream.transferTo(java.io.OutputStream.nullOutputStream())
                } catch (_: Exception) {}
            }
            isRunning = true
            trayIcon?.displayMessage("Reader", "Service started", TrayIcon.MessageType.INFO)
        } catch (e: Exception) {
            trayIcon?.displayMessage("Reader", "Start failed: ${e.message}", TrayIcon.MessageType.ERROR)
        }
    }

    private fun stopService() {
        if (!isRunning) return
        try {
            serviceProcess?.destroy()
            serviceProcess?.waitFor()
        } catch (_: Exception) {}
        serviceProcess = null; isRunning = false
    }

    private fun exitApp() { stopService(); SystemTray.getSystemTray().remove(trayIcon); exitProcess(0) }

    private fun findReaderJar(): File? {
        val dir = File(System.getProperty("user.dir"))
        // dist directory: reader.jar is in the same directory
        val local = File(dir, "reader.jar")
        if (local.exists()) return local
        // development: look in build/libs/
        val libs = File(dir, "build/libs")
        return libs.listFiles()
            ?.filter { it.name.startsWith("reader-") && it.name.endsWith(".jar") }
            ?.filterNot { it.name.contains("plain") || it.name.contains("tray") }
            ?.sortedByDescending { it.lastModified() }
            ?.firstOrNull()
    }

    private fun checkServiceStatus() {
        serviceProcess?.let {
            try { it.exitValue(); isRunning = false; serviceProcess = null } catch (_: IllegalThreadStateException) { isRunning = true }
        } ?: run { isRunning = false }
    }
}
