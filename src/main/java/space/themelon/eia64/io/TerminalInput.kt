package space.themelon.eia64.io

import java.io.InputStream

class TerminalInput(val input: InputStream): InputStream() {
    override fun read(): Int {
        val read = input.read()
        if (read.toChar() == '\r') return '\n'.code
        return read
    }
}