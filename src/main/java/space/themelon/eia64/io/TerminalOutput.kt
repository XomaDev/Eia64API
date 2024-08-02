package space.themelon.eia64.io

import java.io.OutputStream

class TerminalOutput(private val output: OutputStream): OutputStream() {

    override fun write(b: Int) {
        output.apply {
            if (b.toChar() == '\n') {
                write('\r'.code)
            }
            write(b)
            flush()
        }
    }
}