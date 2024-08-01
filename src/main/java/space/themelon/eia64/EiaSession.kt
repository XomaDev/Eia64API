package space.themelon.eia64

import org.apache.sshd.server.ExitCallback
import space.themelon.eia64.TerminalColors.BLUE
import space.themelon.eia64.TerminalColors.BOLD
import space.themelon.eia64.TerminalColors.RED
import space.themelon.eia64.TerminalColors.RESET
import space.themelon.eia64.TerminalColors.YELLOW
import space.themelon.eia64.io.AutoCloseExecutor
import space.themelon.eia64.io.TerminalInput
import space.themelon.eia64.io.TerminalOutput
import space.themelon.eia64.runtime.Executor
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class EiaSession(
    input: TerminalInput,
    private val output: TerminalOutput,
    private val exitCallback: ExitCallback?
) {

    init {
        try {
            serve(input)
        } catch (t: Throwable) {
            // We cannot affect the main thread at any cost
            t.printStackTrace()
        }
    }

    private fun runSafely(output: TerminalOutput, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            output.write("${e.message.toString()}\n".encodeToByteArray())
        }
    }

    private fun serve(input: TerminalInput) {
        val executor = AtomicReference(Executor())
        val shutdown = AtomicBoolean(false)

        val doShutdown: (Int) -> Unit = {
            executor.get().shutdownEvaluator()
            shutdown.set(true)
            exitCallback?.onExit(0)
        }

        // now we have to override default exitProcess() behaviour, we cant let it
        // shut down the whole server lol, when someone does exit(n) in Eia
        Executor.EIA_SHUTDOWN = doShutdown

        AutoCloseExecutor {
            output.write(MESSAGE_MAX_DURATION)
            doShutdown(1)
        }

        output.write(INTRO)
        output.write(SHELL_STYLE)

        // Provide access to standard I/O Stream
        executor.get().apply {
            standardInput = input.input // Provide direct access to the underlying stream
            standardOutput = PrintStream(output)
        }

        val helper = CompletionHelper(
            ready = { tokens ->
                output.write(OUTPUT_STYLE)
                runSafely(output) {
                    executor.get().loadMainTokens(tokens)
                }
                output.write(SHELL_STYLE)
            },
            syntaxError = { error ->
                output.write(ERROR_OUTPUT_STYLE)
                output.write("$error\n".encodeToByteArray())
                output.write(SHELL_STYLE)
            }
        )

        fun useLine(line: String) {
            if (line == "debug") {
                // toggles the debug mode
                Executor.DEBUG = !Executor.DEBUG
                output.write(SHELL_STYLE)
            } else if (!helper.addLine(line)) {
                // There's some more code that needs to be typed in
                output.write(PENDING_SHELL_STYLE)
            }
        }

        val lineBuffer = StringBuilder()
        while (true) {
            val read = input.read()
            output.write(read) // we have to echo back the input

            when (val char = read.toChar()) {
                '\n' -> {
                    val buffer = lineBuffer.toString()
                    useLine(buffer)
                    lineBuffer.clear()
                }
                '\u0003' -> {
                    // Control + C Character
                    // Signal to end the session
                    exitCallback?.onExit(0)
                    break
                }
                '\u007F' -> {
                    // Delete Character
                    if (lineBuffer.isNotEmpty()) {
                        lineBuffer.setLength(lineBuffer.length - 1)
                        output.write(DELETE_CODE)
                    }
                }
                '\u001B' -> {
                    // this is a control character (Escape), we simply discard it
                    // to not cause problems
                    input.read()
                    input.read()
                }
                else -> lineBuffer.append(char)
            }
        }
    }

    companion object {
        private const val EIA_VERSION = 2.1
        private val INTRO = """
            Eia64 Dev $EIA_VERSION
            Type "debug" to toggle debug mode
            
            
        """.trimIndent().encodeToByteArray()
        private val MESSAGE_MAX_DURATION = "Maximum allowed duration of session was reached".encodeToByteArray()

        private val DELETE_CODE = "\b \b".encodeToByteArray()

        private val SHELL_STYLE = "${YELLOW}eia>$RESET ".toByteArray()
        private val PENDING_SHELL_STYLE = ".... ".toByteArray()

        private val OUTPUT_STYLE = "$BLUE$BOLD".encodeToByteArray()
        private val ERROR_OUTPUT_STYLE = "$RED$BOLD".encodeToByteArray()
    }
}