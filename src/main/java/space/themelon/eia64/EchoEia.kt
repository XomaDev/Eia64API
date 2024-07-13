package space.themelon.eia64

import space.themelon.eia64.EiaText.BOLD
import space.themelon.eia64.EiaText.RED
import space.themelon.eia64.EiaText.RESET
import space.themelon.eia64.EiaText.SHELL_STYLE
import space.themelon.eia64.runtime.Executor
import java.io.*
import java.net.ServerSocket
import kotlin.concurrent.thread

object EchoEia {

    fun main() {
        val server = ServerSocket(2244)
        while (true) {
            val client = server.accept()
            thread {
                AutoCloseSocket(client)
                Safety.safeServe(client) { input, output -> initSession(input, output) }
            }
        }
    }

    private fun initSession(input: InputStream, output: OutputStream) {
        output.write(EiaText.INTRO.encodeToByteArray())

        val executor = Executor()
        executor.standardOutput = PrintStream(output)
        executor.standardInput = input

        output.write(SHELL_STYLE)

        val lineBytes = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b == -1) break
            if (b.toChar() == '\n') {
                val code = String(lineBytes.toByteArray())
                lineBytes.reset()

                output.write("$RED$BOLD".encodeToByteArray())
                executor.loadMainSource(code)

                output.write(RESET.encodeToByteArray())
                output.write(SHELL_STYLE)
            } else {
                lineBytes.write(b)
            }
        }
    }
}