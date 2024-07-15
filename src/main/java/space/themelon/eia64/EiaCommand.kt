package space.themelon.eia64

import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import org.apache.sshd.server.channel.ChannelSession
import org.apache.sshd.server.command.Command
import space.themelon.eia64.io.Safety
import space.themelon.eia64.io.TerminalInput
import space.themelon.eia64.io.TerminalOutput
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread

class EiaCommand(private val callback: (TerminalInput, TerminalOutput, ExitCallback?) -> Unit) : Command {

    companion object {
        var activeConnectionsCount = 0
    }

    private lateinit var input: InputStream
    private lateinit var output: OutputStream
    private lateinit var error: OutputStream

    private var exitCallback: ExitCallback? = null

    override fun setInputStream(input: InputStream) {
        this.input = input
    }

    override fun setOutputStream(output: OutputStream) {
        this.output = output
    }

    override fun setErrorStream(error: OutputStream) {
        this.error = error
    }

    override fun setExitCallback(callback: ExitCallback?) {
        exitCallback = callback
    }

    override fun start(channel: ChannelSession?, env: Environment?) {
        val clientAddress = channel?.session?.clientAddress.toString()
        println("New connection from $clientAddress")

        activeConnectionsCount++
        thread {
            try {
                callback(TerminalInput(input), TerminalOutput(output), exitCallback)
            } finally {
                closeStreams()
            }
        }
    }

    override fun destroy(channel: ChannelSession?) {
        activeConnectionsCount--
        if (activeConnectionsCount < 0)
            activeConnectionsCount = 0
        closeStreams()
    }

    private fun closeStreams() {
        Safety.safeClose(input)
        Safety.safeClose(output)
        Safety.safeClose(error)
    }

}