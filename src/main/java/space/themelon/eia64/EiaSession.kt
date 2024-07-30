package space.themelon.eia64

import org.apache.sshd.server.ExitCallback
import space.themelon.eia64.EiaLive.Companion
import space.themelon.eia64.TerminalColors.BLUE
import space.themelon.eia64.TerminalColors.BOLD
import space.themelon.eia64.TerminalColors.RED
import space.themelon.eia64.TerminalColors.RESET
import space.themelon.eia64.TerminalColors.YELLOW
import space.themelon.eia64.analysis.ParserX
import space.themelon.eia64.io.AutoCloseExecutor
import space.themelon.eia64.io.TerminalInput
import space.themelon.eia64.io.TerminalOutput
import space.themelon.eia64.runtime.Executor
import space.themelon.eia64.syntax.Lexer
import java.io.PrintStream
import java.util.*
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

        fun writeShell() {
            output.write(SHELL_STYLE)
            //if (!lineByLine)
                //output.write("\r\n".encodeToByteArray())
        }
        output.write(INTRO)
        writeShell()
        //output.slowAnimate = false

        // Provide access to standard I/O Stream
        executor.get().apply {
            standardInput = input.input // Provide direct access to the underlying stream
            standardOutput = PrintStream(output)
        }

//        val codeArray = CodeArray()

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

//        fun getCodeString(): String? {
//            val code = String(codeArray.get())
//                // make sure to remove any control characters present
//                .replace(Regex("\\p{Cntrl}"), "")
//                .trim()
//            if (code.isEmpty()) return null
//            return code
//        }

//        fun execute() {
//            val filteredCode = getCodeString() ?: return
//            helper.addLine()
//            codeArray.reset()
//            output.write(OUTPUT_STYLE)
//            runSafely(output) {
//                executor.get().loadMainSource(filteredCode)
//            }
//            writeShell()
//        }

//        fun lex() {
//            val filteredCode = getCodeString() ?: return
//            codeArray.reset()
//            output.write(OUTPUT_STYLE)
//            output.write(10)
//
//            runSafely(output) {
//                Lexer(filteredCode).tokens.forEach {
//                    output.write(it.toString().encodeToByteArray())
//                    output.write(10)
//                }
//            }
//
//            writeShell()
//        }

//        fun parse() {
//            val filteredCode = getCodeString() ?: return
//            codeArray.reset()
//            output.write(OUTPUT_STYLE)
//            output.write(10)
//
//            runSafely(output) {
//                val tokens = Lexer(filteredCode).tokens
//                val trees = ParserX(Executor()).parse(tokens)
//
//                trees.expressions.forEach {
//                    output.write(it.toString().encodeToByteArray())
//                    output.write(10)
//                }
//            }
//
//            writeShell()
//        }

//        while (!shutdown.get()) {
//            val letterCode = input.read()
//            if (letterCode == -1) {
//                exitCallback?.onExit(0)
//                break
//            }
//
//            when (val char = letterCode.toChar()) {
//                '\u007F' -> {
//                    // Delete Character
//                    if (codeArray.isNotEmpty()) {
//                        output.write(DELETE_CODE)
//                        codeArray.delete()
//                    }
//                }
//
//                '\u0003' -> {
//                    // Control + C Character
//                    // Signal to end the session
//                    exitCallback?.onExit(0)
//                    break
//                }
//
//                '\u000C' -> {
//                    // Control + L
//                    // Request to lex the tokens and print them
//                    lex()
//                }
//
//                '\u0010' -> {
//                    // Control + P
//                    // To print the parsed nodes
//                    parse()
//                }
//
//                '\u001B' -> {
//                    // this is a control character (Escape), we simply discard it
//                    // to not cause problems
//                    input.read()
//                    input.read()
//                }
//
//                else -> {
//                    output.write(letterCode)
//                    if (char == '\n') {
//                        execute()
//                        continue
//                    }
//                    codeArray.put(letterCode.toByte())
//                }
//            }
//        }

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
        private val INTRO = """
            Eia64 Dev 2.1
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