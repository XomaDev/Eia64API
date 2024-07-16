package space.themelon.eia64

import org.apache.sshd.server.ExitCallback
import space.themelon.eia64.EiaText.BOLD
import space.themelon.eia64.EiaText.CYAN
import space.themelon.eia64.EiaText.RED
import space.themelon.eia64.EiaText.SHELL_STYLE
import space.themelon.eia64.analysis.Parser
import space.themelon.eia64.io.AutoCloseExecutor
import space.themelon.eia64.io.TerminalInput
import space.themelon.eia64.io.TerminalOutput
import space.themelon.eia64.runtime.Executor
import space.themelon.eia64.syntax.Lexer
import java.io.PrintStream
import java.util.StringJoiner

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

        val note = if (lineByLine)
            "Line-by-Line Interpretation mode"
        else "Use Control-E to run the code"

        output.write(
            EiaText.INTRO
                .replace("&", EiaCommand.activeConnectionsCount.toString())
                .replace("%1", "\t⭐\uFE0F$note\r\n\r\n")
                .encodeToByteArray()
        )

        output.write(SHELL_STYLE)
        if (!lineByLine)
            output.write("\r\n".encodeToByteArray())
        output.slowAnimate = false

        // Provide access to standard I/O Stream
        executor.standardInput = input.input // Provide direct access to the underlying stream
        executor.standardOutput = PrintStream(output)

        val codeArray = EByteArray()

        fun getFilteredCode(): String {
            return String(codeArray.get())
                // make sure to remove any control characters present
                .replace(Regex("\\p{Cntrl}"), "")
                .trim()
        }

        fun execute() {
            val filteredCode = getFilteredCode()
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
            if (!lineByLine) {
                output.write("\r\n".encodeToByteArray())
            }
        }

        fun lex() {
            val filteredCode = getFilteredCode()
            if (filteredCode.isEmpty()) {
                return
            }
            codeArray.reset()
            output.write(OUTPUT_STYLE)
            output.write(10)
            val lines = StringJoiner("\n")
            Lexer(filteredCode).tokens.forEach { lines.add(it.toString()) }
            output.write((lines.toString() + "\n").encodeToByteArray())

            output.write(SHELL_STYLE)
            if (!lineByLine) {
                output.write("\r\n".encodeToByteArray())
            }
        }

        fun parse() {
            println(String(codeArray.get()))
            val filteredCode = getFilteredCode()
            if (filteredCode.isEmpty()) {
                return
            }
            codeArray.reset()
            output.write(OUTPUT_STYLE)
            output.write(10)

            val tokens = Lexer(filteredCode).tokens
            val trees = Parser(Executor()).parse(tokens)

            trees.expressions.forEach {
                output.write(it.toString().encodeToByteArray())
                output.write(10)
            }

            output.write(SHELL_STYLE)
            if (!lineByLine) {
                output.write("\r\n".encodeToByteArray())
            }
        }

        while (true) {
            val letterCode = input.read()
            if (letterCode == -1) {
                exitCallback?.onExit(0)
                break
            }

            println(letterCode.toChar().code.toString() + " | " + letterCode.toChar())

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

                '\u000C' -> {
                    // Control + L
                    // Request to lex the tokens and print them
                    lex()
                }

                '\u0010' -> {
                    // Control + P
                    // To print the parsed nodes
                    parse()
                }

                '\u001B' -> {
                    // this is a control character (Escape), we simply discard it
                    // to not cause problems
                    input.read()
                    input.read()
                }

                '\u000E' -> {
                    // Control + N
                    // Request for a new session

                    // TODO
                    // write Executor code to reset memory
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
        private val OUTPUT_STYLE = "$CYAN$BOLD".encodeToByteArray()
    }
}