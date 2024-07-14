package space.themelon.eia64

import java.io.InputStream

class TerminalInput(private val input: InputStream): InputStream() {
    override fun read(): Int {
        val read = input.read()
        if (read.toChar() == '\r') {
            return '\n'.code
        }
        return read
    }
}