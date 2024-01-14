package org.example.server;

import java.util.function.BiFunction;

import org.reactivestreams.Publisher;

import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

public interface WSHandler {
	String path();
	BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> handler();
}
