package space.themelon.eia64

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

object Safety {

    fun safeClose(client: Socket) {
        try {
            client.close()
        } catch (ignored: IOException) { }
    }

    fun safeServe(client: Socket, block: (InputStream, OutputStream) -> Unit) {
        val output = client.getOutputStream()
        try {
            block(client.getInputStream(), output)
        } catch (io: Exception) {
            try {
                output.write("\nCaught Error ${io.message}".encodeToByteArray())
            } catch (ignored: IOException) { }
            safeClose(client)
        }
    }
}