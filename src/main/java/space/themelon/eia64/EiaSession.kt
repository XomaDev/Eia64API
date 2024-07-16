package space.themelon.eia64

import org.apache.sshd.server.ExitCallback
import space.themelon.eia64.EiaText.BOLD
import space.themelon.eia64.EiaText.CYAN
import space.themelon.eia64.EiaText.SHELL_STYLE
import space.themelon.eia64.analysis.Parser
import space.themelon.eia64.io.AutoCloseExecutor
import space.themelon.eia64.io.TerminalInput
import space.themelon.eia64.io.TerminalOutput
import space.themelon.eia64.runtime.Executor
import space.themelon.eia64.syntax.Lexer
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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

    private fun safeRun(output: TerminalOutput, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            output.write("${e.message.toString()}\n".encodeToByteArray())
        }
    }

    private fun serve(input: TerminalInput, lineByLine: Boolean) {
        // we may change an Executor object, so we should use atomic reference
        val executor = AtomicReference(Executor())

        // for additional checks
        val shutdown = AtomicBoolean(false)

        AutoCloseExecutor {
            output.write(MESSAGE_MAX_DURATION)
            executor.get().shutdownEvaluator()
            shutdown.set(true)
            exitCallback?.onExit(0)
        }

        val note = if (lineByLine) "Line-by-Line Interpretation mode"
        else "Use Control-E to run the code"

        output.write(
            EiaText.INTRO
                .replace("&", EiaCommand.activeConnectionsCount.toString())
                .replace("%1", "\t⭐\uFE0F$note\r\n\r\n")
                .encodeToByteArray()
        )

        fun writeShell() {
            output.write(SHELL_STYLE)
            if (!lineByLine)
                output.write("\r\n".encodeToByteArray())
        }
        writeShell()
        //output.slowAnimate = false

        // Provide access to standard I/O Stream
        executor.get().apply {
            standardInput = input.input // Provide direct access to the underlying stream
            standardOutput = PrintStream(output)
        }

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
            safeRun(output) {
                executor.get().loadMainSource(filteredCode)
            }
            writeShell()
        }

        fun lex() {
            val filteredCode = getFilteredCode()
            if (filteredCode.isEmpty()) {
                return
            }
            codeArray.reset()
            output.write(OUTPUT_STYLE)
            output.write(10)

            safeRun(output) {
                Lexer(filteredCode).tokens.forEach {
                    output.write(it.toString().encodeToByteArray())
                    output.write(10)
                }
            }

            writeShell()
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

            safeRun(output) {
                val tokens = Lexer(filteredCode).tokens
                val trees = Parser(Executor()).parse(tokens)

                trees.expressions.forEach {
                    output.write(it.toString().encodeToByteArray())
                    output.write(10)
                }
            }

            writeShell()
        }

        while (!shutdown.get()) {
            val letterCode = input.read()
            if (letterCode == -1) {
                exitCallback?.onExit(0)
                break
            }

            //debug println(letterCode.toChar().code.toString() + " | " + letterCode.toChar())

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
                    // Ctrl + N
                    // Clears the memory, a fresh session

                    // Note: We should not cal clearMemory() on executor
                    // rather create a new instance
                    executor.get().shutdownEvaluator()
                    executor.set(Executor().apply {
                        standardInput = input.input // Provide direct access to the underlying stream
                        standardOutput = PrintStream(output)
                    })

                    output.write(OUTPUT_STYLE)
                    output.write(10)
                    output.write(MESSAGE_MEM_CLEARED)

                    writeShell()
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
        private val MESSAGE_MEM_CLEARED = "Memory was cleared\n".encodeToByteArray()

        private val DELETE_CODE = "\b \b".encodeToByteArray()
        private val OUTPUT_STYLE = "$CYAN$BOLD".encodeToByteArray()
    }
}