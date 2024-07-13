package space.themelon.eia64

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

    private val scope = CoroutineScope(Dispatchers.Default)
    private val channel = Channel<Socket>()

    init {
        scope.launch {
            // 5 mins = 5 * 60 * 1000
            delay(5 * 60 * 1000)
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