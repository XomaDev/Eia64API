package space.themelon.eia64

import org.apache.sshd.server.ExitCallback
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.UserAuthNoneFactory
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.shell.ShellFactory
import space.themelon.eia64.EiaText.BLUE
import space.themelon.eia64.EiaText.BOLD
import space.themelon.eia64.EiaText.RED
import space.themelon.eia64.EiaText.RESET
import space.themelon.eia64.EiaText.SHELL_STYLE
import space.themelon.eia64.io.AutoCloseExecutor
import space.themelon.eia64.io.TerminalInput
import space.themelon.eia64.io.TerminalOutput
import space.themelon.eia64.runtime.Executor
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.file.Paths
import java.util.*

object EiaPlayground {

    fun main() {
        SshServer.setUpDefaultServer().apply {
            port = 2103
            keyPairProvider = SimpleGeneratorHostKeyProvider(Paths.get("eiaplayground.ser"))
            userAuthFactories = listOf(UserAuthNoneFactory.INSTANCE)
            shellFactory = ShellFactory { EiaCommand(initSession) }
            start()
        }
    }

    private val initSession: (TerminalInput, TerminalOutput, ExitCallback?) -> Unit = { input, output, exitCallback ->
        output.write(EiaText.INTRO.encodeToByteArray())
        output.write("\t⭐\uFE0FUse Control-E to run the code\r\n\r\n".encodeToByteArray())
        output.write(SHELL_STYLE)
        output.write("\r\n".encodeToByteArray())
        output.slowAnimate = false

        val executor = Executor()
        executor.standardOutput = PrintStream(output)
        executor.standardInput = input.input

        AutoCloseExecutor(executor) {
            output.write("Max session duration of 5 mins reached\n".encodeToByteArray())
            output.close()
            input.close()
            exitCallback?.onExit(0)
        }

        val array = EByteArray()
        while (true) {
            val b = input.read()
            if (b == -1) {
                exitCallback?.onExit(0)
                break
            }
            if (b.toChar() == '\u007F') {
                // delete character
                if (array.isNotEmpty()) {
                    output.write('\b'.code)
                    output.write(' '.code)
                    output.write('\b'.code)
                    array.delete()
                }
                continue
            }
            if (b.toChar() == '\u0003') {
                // Control + C
                exitCallback?.onExit(0)
                break
            }
            output.write(b)

            if (b.toChar() == '\u0005') {
                // Control E character

                val code = String(array.get())
                    // this removes any control characters present
                    .replace(Regex("\\p{Cntrl}"), "")
                array.reset()
                output.write("$RED$BOLD".encodeToByteArray())
                try {
                    executor.loadMainSource(code)
                } catch (e: Exception) {
                    output.write("${e.message}\n".encodeToByteArray())
                }

                output.write(SHELL_STYLE)
                output.write("\r\n".encodeToByteArray())

            } else {
                array.put(b.toByte())
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