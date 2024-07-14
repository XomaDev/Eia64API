package space.themelon.eia64

import org.apache.sshd.server.ExitCallback
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.UserAuthNoneFactory
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.shell.ShellFactory
import space.themelon.eia64.EiaText.BOLD
import space.themelon.eia64.EiaText.RED
import space.themelon.eia64.EiaText.RESET
import space.themelon.eia64.EiaText.SHELL_STYLE
import space.themelon.eia64.runtime.Executor
import java.io.*
import java.nio.file.Paths

object EiaShell {

    fun main() {
        SshServer.setUpDefaultServer().apply {
            port = 2244
            keyPairProvider = SimpleGeneratorHostKeyProvider(Paths.get("eiashell.ser"))
            userAuthFactories = listOf(UserAuthNoneFactory.INSTANCE)
            shellFactory = ShellFactory { EiaCommand(initSession) }
            start()
        }
    }

    private val initSession: (InputStream, OutputStream, ExitCallback?) -> Unit = { input, output, exitCallback ->
        output.write(EiaText.INTRO.encodeToByteArray())

        val executor = Executor()
        executor.standardOutput = PrintStream(output)
        executor.standardInput = input

        output.write(SHELL_STYLE)

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

            if (b.toChar() == '\n') {
                val code = String(array.get())
                    // this removes any control characters present
                    .replace(Regex("\\p{Cntrl}"), "")
                array.reset()
                output.write("$RED$BOLD".encodeToByteArray())
                executor.loadMainSource(code)

                output.write(RESET.encodeToByteArray())
                output.write(SHELL_STYLE)
            } else {
                array.put(b.toByte())
            }
        }
    }
}
