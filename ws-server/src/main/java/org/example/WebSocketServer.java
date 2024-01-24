package org.example;

import org.example.component.DaggerWSServerComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketServer.class);

	public static void main(String[] args) {
		var serverComponent = DaggerWSServerComponent.create();
		LOGGER.info("Starting event read process in separate thread...");
		Thread.startVirtualThread(serverComponent.documentEventsReadProcess());

		LOGGER.info("Starting protocol switch process...");
		Thread.startVirtualThread(serverComponent.protocolSwitchProcess());

		LOGGER.info("Starting HTTP server in current thread...");
		var server = serverComponent.webSocketServer().bindNow();

		LOGGER.info("Bound server to TCP port, start accepting connections...");
		server.onDispose().block();
	}

}
