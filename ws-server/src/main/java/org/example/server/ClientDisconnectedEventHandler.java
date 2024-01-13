package org.example.server;

import reactor.netty.http.websocket.WebsocketOutbound;

public interface ClientDisconnectedEventHandler {
	void onClientDisconnect(int documentId, WebsocketOutbound wsOutbound);
}
