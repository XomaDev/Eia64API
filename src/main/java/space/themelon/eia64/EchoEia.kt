package space.themelon.eia64

import space.themelon.eia64.runtime.Executor
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

object EchoEia {

    fun main() {
        val server = ServerSocket(2244)
        while (true) {
            val client = server.accept()
            thread {
                serveClient(client)
            }
        }
    }

    private fun serveClient(client: Socket) {
        val output = client.getOutputStream()
        try {
            initSession(client.getInputStream(), output)
        } catch (io: Exception) {
            output.write("\nCaught Error ${io.message}".encodeToByteArray())
            Safety.safeClose(client)
        }
    }

    private fun initSession(input: InputStream, output: OutputStream) {
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