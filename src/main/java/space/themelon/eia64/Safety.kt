package space.themelon.eia64

import java.io.IOException
import java.net.Socket

object Safety {
    fun safeClose(socket: Socket) {
        try {
            socket.close()
        } catch (ignored: IOException) {}
    }
}