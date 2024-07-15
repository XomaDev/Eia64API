package space.themelon.eia64

import org.apache.sshd.server.ExitCallback
import space.themelon.eia64.EiaText.BOLD
import space.themelon.eia64.EiaText.RED
import space.themelon.eia64.EiaText.SHELL_STYLE
import space.themelon.eia64.io.AutoCloseExecutor
import space.themelon.eia64.io.TerminalInput
import space.themelon.eia64.io.TerminalOutput
import space.themelon.eia64.runtime.Executor
import java.io.PrintStream

class EiaSession(
    lineByLine: Boolean,
    input: TerminalInput,
    private val output: TerminalOutput,
    private val exitCallback: ExitCallback?
) {
    init {
        try {
            serve(input, lineByLine)
        } catch (t: Throwable) {
            // We cannot affect the main thread at any cost
            t.printStackTrace()
        }
    }

    private fun serve(input: TerminalInput, lineByLine: Boolean) {
        val executor = Executor()

        AutoCloseExecutor(executor) {
            output.write(MESSAGE_MAX_DURATION)
            exitCallback?.onExit(0)
        }

        output.write(
            EiaText.INTRO
                .replace("&", EiaCommand.activeConnectionsCount.toString())
                .encodeToByteArray()
        )
        val note = if (lineByLine)
            "Line-by-Line Interpretation mode"
        else "Use Control-E to run the code"
        output.write("\t⭐\uFE0F$note\r\n\r\n".encodeToByteArray())
        output.write(SHELL_STYLE)
        output.write("\r\n".encodeToByteArray())
        output.slowAnimate = false

        // Provide access to standard I/O Stream
        executor.standardInput = input.input // Provide direct access to the underlying stream
        executor.standardOutput = PrintStream(output)

        val codeArray = EByteArray()

        fun execute() {
            if (codeArray.isNotEmpty()) {
                val filteredCode = String(codeArray.get())
                    // make sure to remove any control characters present
                    .replace(Regex("\\p{Cntrl}"), "")
                    .trim()
                if (filteredCode.isEmpty()) {
                    return
                }
                codeArray.reset()
                output.write(OUTPUT_STYLE)
                try {
                    executor.loadMainSource(filteredCode)
                } catch (e: Exception) {
                    output.write("${e.message}\n".encodeToByteArray())
                }
                output.write(SHELL_STYLE)
                output.write("\r\n".encodeToByteArray())
            }
        }

        while (true) {
            val letterCode = input.read()
            if (letterCode == -1) {
                exitCallback?.onExit(0)
                break
            }

            when (val char = letterCode.toChar()) {
                '\u007F' -> {
                    // Delete Character
                    if (codeArray.isNotEmpty()) {
                        output.write(DELETE_CODE)
                        codeArray.delete()
                    }
                }

                '\u0003' -> {
                    // Control + C Character
                    // Signal to end the session
                    exitCallback?.onExit(0)
                    break
                }

                '\u0005' -> {
                    // Control + E Character
                    execute()
                }

                else -> {
                    output.write(letterCode)
                    if (lineByLine && char == '\n') {
                        execute()
                        continue
                    }
                    codeArray.put(letterCode.toByte())
                }
            }
        }
    }

    companion object {
        private val MESSAGE_MAX_DURATION = "Maximum allowed duration of session was reached".encodeToByteArray()
        private val DELETE_CODE = "\b \b".encodeToByteArray()
        private val OUTPUT_STYLE = "$RED$BOLD".encodeToByteArray()
    }
}