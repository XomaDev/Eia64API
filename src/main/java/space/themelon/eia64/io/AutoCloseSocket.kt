package space.themelon.eia64.io

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.Socket

class AutoCloseSocket(
    socket: Socket
) {

    companion object {
        // 5 minutes
        const val TIMEOUT = 5 * 50 * 1000
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private val channel = Channel<Socket>()

    init {
        scope.launch {
            delay(TIMEOUT.toLong())
            channel.send(socket)
        }

        scope.launch {
            for (sock in channel) {
                try {
                    socket.getOutputStream().write("\n[Reached max session period]".encodeToByteArray())
                } catch (ignored: IOException) {

                }
                Safety.safeClose(socket)
            }
        }
    }
}