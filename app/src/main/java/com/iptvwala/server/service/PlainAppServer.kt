package com.iptvwala.server.service

import com.iptvwala.server.handler.ApiHandler
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import fi.iki.elonen.NanoWSD.WebSocket
import fi.iki.elonen.NanoWSD.WebSocketFrame
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode
import java.io.IOException

class PlainAppServer(
    port: Int,
    private val apiHandler: ApiHandler,
    private val serverState: ServerState
) : NanoWSD(port) {

    override fun serveHttp(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method.name
        val params = session.parameters.mapValues { it.value.firstOrNull() ?: "" }
        
        return apiHandler.handleRequest(uri, method, params) 
            ?: super.serveHttp(session)
    }

    override fun openWebSocket(handshake: IHTTPSession): WebSocket {
        return object : WebSocket(handshake) {
            private val client = WebSocketClient()

            override fun onOpen() {
                client.setSocket(this)
                serverState.addWebSocketClient(client)
            }

            override fun onClose(code: CloseCode, reason: String, remote: Boolean) {
                serverState.removeWebSocketClient(client)
            }

            override fun onMessage(message: WebSocketFrame) {
                // Handle incoming messages if needed
            }

            override fun onPong(pong: WebSocketFrame) {
            }

            override fun onException(exception: IOException) {
                serverState.removeWebSocketClient(client)
            }
        }
    }
}
