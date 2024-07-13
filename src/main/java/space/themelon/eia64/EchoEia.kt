package space.themelon.eia64

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
        val codeOutput = ByteArrayOutputStream()
        executor.STANDARD_OUTPUT = PrintStream(codeOutput)

        output.write("eia $ ".toByteArray())

        val lineBytes = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b == -1) break
            if (b.toChar() == '\n') {
                val code = String(lineBytes.toByteArray())
                lineBytes.reset()
                executor.loadMainSource(code)
                output.write(codeOutput.toByteArray())
                codeOutput.reset()
                output.write("eia $ ".toByteArray())
            } else {
                lineBytes.write(b)
            }
        }
    }
}