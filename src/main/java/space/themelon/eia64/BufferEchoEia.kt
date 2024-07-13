package space.themelon.eia64

import space.themelon.eia64.EiaText.SHELL_STYLE
import space.themelon.eia64.runtime.Executor
import java.io.*
import java.net.ServerSocket
import java.util.*
import kotlin.concurrent.thread

object BufferEchoEia {

    fun main() {
        val server = ServerSocket(2103)
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
        output.write(("\t⭐\uFE0F Running in buffer mode, type ~~ to run code." +
                "\n\t✏\uFE0F For line-by-line execution use port 2244\n\n").encodeToByteArray())
        val executor = Executor()
        executor.standardOutput = PrintStream(output)
        executor.standardInput = input

        val scanner = Scanner(input)
        var buffer = StringJoiner("\n")
        output.write(SHELL_STYLE)
        while (true) {
            val line = scanner.nextLine()
            if (line == "exit") break
            else if (line == "~~") {
                println(buffer)
                executor.loadMainSource(buffer.toString())
                buffer = StringJoiner("\n")
                output.write(SHELL_STYLE)
            } else buffer.add(line)
        }
    }
}