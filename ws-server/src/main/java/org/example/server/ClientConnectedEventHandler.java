package org.example.server;

import reactor.netty.http.websocket.WebsocketOutbound;

public interface ClientConnectedEventHandler {
	void onClientConnect(int documentId, WebsocketOutbound wsOutbound);
}
