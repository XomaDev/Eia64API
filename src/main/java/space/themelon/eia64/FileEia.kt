package space.themelon.eia64

import space.themelon.eia64.runtime.Executor
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InterruptedIOException
import java.io.OutputStream
import java.io.PrintStream
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import kotlin.concurrent.thread

object FileEia {
    fun main() {
        val server = ServerSocket(7878)
        while (true) {
            val client = server.accept()
            thread {
                AutoCloseSocket(client)
                Safety.safeServe(client) { input, output -> serveClient(client, input, output)}
            }
        }
    }

    private fun serveClient(client: Socket, input: InputStream, output: OutputStream) {
        val scanner = BufferedInputStream(input)
        val bytes = ByteArrayOutputStream()
        while (true) {
            try {
                bytes.write(scanner.read())
            } catch (interrupt: InterruptedIOException) {
                break
            }
        }
        println(bytes)

        val executor = Executor()
        executor.standardOutput = PrintStream(output)
        executor.standardInput = BufferedInputStream(input)

        executor.loadMainSource(bytes.toString())
    }
}