package space.themelon.eia64

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
        output.write("      ⭐\uFE0F Running in buffer mode, please type ~~ to run code\n\n".encodeToByteArray())
        val executor = Executor()
        val codeOutput = ByteArrayOutputStream()
        executor.STANDARD_OUTPUT = PrintStream(codeOutput)

        val scanner = Scanner(input)
        var buffer = StringJoiner("\n")
        while (true) {
            output.write("eia > ".toByteArray())
            val line = scanner.nextLine()
            if (line == "exit") break
            else if (line == "~~") {
                executor.loadMainSource(buffer.toString())
                output.write(codeOutput.toByteArray())
                codeOutput.reset()

                buffer = StringJoiner("\n")
            }
            else buffer.add(line)
        }
    }
}