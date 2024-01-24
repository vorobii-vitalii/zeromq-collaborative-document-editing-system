package org.example.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import reactor.netty.http.websocket.WebsocketOutbound;

class TestWebSocketServerModule {
	private static final int DOC_ID = 123;
	public static final int PORT = 2900;

	WebSocketServerModule webSocketServerModule = new WebSocketServerModule();

	@Test
	void webSocketServer() {
		var server = webSocketServerModule.webSocketServer(Set.of(), PORT);
		assertThat(server).isNotNull();
	}

	@Test
	void getClientDisconnectedEventHandler() {
		WebsocketOutbound websocketOutbound1 = mock(WebsocketOutbound.class);
		WebsocketOutbound websocketOutbound2 = mock(WebsocketOutbound.class);
		ConcurrentHashMap<Integer, List<WebsocketOutbound>> subscribersByDocumentId = new ConcurrentHashMap<>();
		subscribersByDocumentId.put(DOC_ID, new ArrayList<>(List.of(websocketOutbound1, websocketOutbound2)));
		var handler = webSocketServerModule.getClientDisconnectedEventHandler(subscribersByDocumentId);
		handler.onClientDisconnect(DOC_ID, websocketOutbound1);
		handler.onClientDisconnect(DOC_ID, websocketOutbound2);
		assertThat(subscribersByDocumentId.get(DOC_ID)).isEmpty();
	}

	@Test
	void getClientConnectedEventHandler() {
		WebsocketOutbound websocketOutbound1 = mock(WebsocketOutbound.class);
		WebsocketOutbound websocketOutbound2 = mock(WebsocketOutbound.class);
		ConcurrentHashMap<Integer, List<WebsocketOutbound>> subscribersByDocumentId = new ConcurrentHashMap<>();
		var handler = webSocketServerModule.getClientConnectedEventHandler(subscribersByDocumentId);
		handler.onClientConnect(DOC_ID, websocketOutbound1);
		handler.onClientConnect(DOC_ID, websocketOutbound2);
		assertThat(subscribersByDocumentId.get(DOC_ID)).containsExactlyInAnyOrder(websocketOutbound1, websocketOutbound2);
	}
}