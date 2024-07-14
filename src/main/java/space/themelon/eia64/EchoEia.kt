package space.themelon.eia64

import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.UserAuthNoneFactory
import org.apache.sshd.server.channel.ChannelSession
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.shell.ShellFactory
import space.themelon.eia64.EiaText.BOLD
import space.themelon.eia64.EiaText.RED
import space.themelon.eia64.EiaText.RESET
import space.themelon.eia64.EiaText.SHELL_STYLE
import space.themelon.eia64.runtime.Executor
import java.io.*
import java.nio.file.Paths

object EchoEia {

    fun main() {
        SshServer.setUpDefaultServer().apply {
            port = 2244
            keyPairProvider = SimpleGeneratorHostKeyProvider(Paths.get("hostkey.ser"))
            userAuthFactories = listOf(UserAuthNoneFactory.INSTANCE)
            shellFactory = ShellFactory { EiaShell(initSession) }
            start()
        }
    }

    private val initSession: (InputStream, OutputStream, ExitCallback?) -> Unit = { input, output, exitCallback ->
        output.write(EiaText.INTRO.encodeToByteArray())
//        output.flush()

        val executor = Executor()
        executor.standardOutput = PrintStream(output)
        executor.standardInput = input

        output.write(SHELL_STYLE)
//        output.flush()

        val lineBytes = ByteArrayOutputStream()
        val length = 0
        while (true) {
            val b = input.read()
            if (b == -1) {
                exitCallback?.onExit(0)
                break
            }
            if (b.toChar() == '') {
//                exitCallback?.onExit(0)
                output.write('\b'.code)
                output.write(' '.code)
                output.write('\b'.code)
//                break
                continue
            }

//            if (b.toChar() == '\r') output.write(10)
            output.write(b)
//            output.flush()

            if (b.toChar() == '\n') {
                println(lineBytes)
                val code = String(lineBytes.toByteArray())
                lineBytes.reset()

                output.write("$RED$BOLD".encodeToByteArray())
//                output.flush()
                executor.loadMainSource(code)

                output.write(RESET.encodeToByteArray())
                output.write(SHELL_STYLE)
//                output.flush()
            } else {
                lineBytes.write(b)
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
