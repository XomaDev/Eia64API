package space.themelon.eia64.io

import java.io.OutputStream

class TerminalOutput(private val output: OutputStream): OutputStream() {

    var slowAnimate = true

    override fun write(b: Int) {
        if (slowAnimate) {
            Thread.sleep(1)
        }
        if (b.toChar() == '\n') {
            output.write('\r'.code)
        }
        output.write(b)
        output.flush()
    }
}