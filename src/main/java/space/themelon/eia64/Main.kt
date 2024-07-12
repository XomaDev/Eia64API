package space.themelon.eia64

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val server = HttpServer.create(InetSocketAddress(7070), 0)
        server.createContext("/", httpHandler)
        server.executor = null
        server.start()
    }

    private val httpHandler = HttpHandler { exchange ->
        if ("POST".equals(exchange.requestMethod, ignoreCase = true)) {
            handleFileUpload(exchange)
        } else {
            val response = "Hello, World".encodeToByteArray()
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use {
                it.write(response)
            }
        }
    }

    private fun handleFileUpload(exchange: HttpExchange) {
        val inputStream = exchange.requestBody
        val content = inputStream.readBytes()
        exchange.sendResponseHeaders(200, content.size.toLong())
        exchange.responseBody.use {
            it.write(content)
        }
    }
}

