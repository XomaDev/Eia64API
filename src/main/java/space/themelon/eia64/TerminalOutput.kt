package space.themelon.eia64

import java.io.OutputStream

class TerminalOutput(private val output: OutputStream): OutputStream() {
    override fun write(b: Int) {
        if (b.toChar() == '\n') {
            output.write('\r'.code)
        }
        output.write(b)
        output.flush()
    }
}