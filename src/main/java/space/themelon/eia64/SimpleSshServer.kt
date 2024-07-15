package space.themelon.eia64

import org.apache.sshd.server.ExitCallback
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.UserAuthNoneFactory
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.shell.ShellFactory
import java.io.*
import java.nio.file.Paths
import kotlin.system.exitProcess

fun main() {
    val sshd = SshServer.setUpDefaultServer()
    sshd.port = 1212  // Change the port number here
    sshd.keyPairProvider = SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser"))

    sshd.userAuthFactories = listOf(UserAuthNoneFactory.INSTANCE)

    sshd.shellFactory = ShellFactory { CustomShell() }

    try {
        sshd.start()
        println("SSH Server started on port 1212")

        // Keep the server running
        while (true) {
            Thread.sleep(Long.MAX_VALUE)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        sshd.stop()
        exitProcess(1)
    }
}


class CustomShell : Command {
    private lateinit var inStream: InputStream
    private lateinit var out: OutputStream
    private lateinit var err: OutputStream
    private var callback: ExitCallback? = null

    override fun setInputStream(inStream: InputStream) {
        this.inStream = inStream
    }

    override fun setOutputStream(out: OutputStream) {
        this.out = out
    }

    override fun setErrorStream(err: OutputStream) {
        this.err = err
    }

    override fun setExitCallback(callback: ExitCallback?) {
        this.callback = callback
    }


    override fun start(channel: org.apache.sshd.server.channel.ChannelSession?, env: org.apache.sshd.server.Environment?) {
        Thread {
            try {
                out.write("Welcome to the custom shell!\n\r".toByteArray())
                out.flush()

                val inputLine = StringBuilder()
                var ch: Int

                while (true) {
                    ch = inStream.read()
                    if (ch == -1) {
                        break
                    }

                    if (ch.toChar() == '\r') {
                        println("yeahh")
                        out.write(10)
                    }
                    out.write(ch)
                    out.flush()
//                    if (ch.toChar() == '\r' || ch.toChar() == '\n') {
//                        println('\r'.code)
//                        if (inputLine.isNotEmpty()) {
//                            val input = inputLine.toString().trim()
//                            out.write("You typed: $input\n\r".toByteArray())
//                            out.flush()
//                            if (input.equals("exit", ignoreCase = true)) {
//                                out.write("Goodbye!\n\r".toByteArray())
//                                out.flush()
//                                callback?.onExit(0)
//                                break
//                            }
//                            inputLine.setLength(0)
//                        }
//                    } else {
//                        inputLine.append(ch.toChar())
//                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                closeResources()
            }
        }.start()
    }

    override fun destroy(channel: org.apache.sshd.server.channel.ChannelSession?) {
        closeResources()
    }

    private fun closeResources() {
        try {
            inStream.close()
            out.close()
            err.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
