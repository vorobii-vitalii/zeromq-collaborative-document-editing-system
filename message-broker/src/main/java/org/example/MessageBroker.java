package org.example;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class MessageBroker {
	private static final String TCP_BIND_ADDRESS_TEMPLATE = "tcp://*:%d";

	public static void main(String[] args) {
		try (var context = new ZContext()) {
			var frontend = context.createSocket(SocketType.ROUTER);
			frontend.bind(TCP_BIND_ADDRESS_TEMPLATE.formatted(getFrontendPort()));

			var backend = context.createSocket(SocketType.DEALER);
			backend.bind(TCP_BIND_ADDRESS_TEMPLATE.formatted(getBackendPort()));

			ZMQ.proxy(frontend, backend, null);
		}
	}

	private static int getFrontendPort() {
		return Integer.parseInt(System.getenv("FRONTEND_PORT"));
	}

	private static int getBackendPort() {
		return Integer.parseInt(System.getenv("BACKEND_PORT"));
	}

}
