package org.example.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.constants.ServerConstants;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

class TestDocumentServerModule {
	private static final String SERVER_ADDRESS = "tcp://*:3139";

	DocumentServerModule documentServerModule = new DocumentServerModule();

	@Test
	void zeroMqContext() {
		assertThat(documentServerModule.zeroMqContext()).isNotNull();
	}

	@Test
	void routerSocket() {
		var context = mock(ZContext.class);
		var socket = mock(ZMQ.Socket.class);
		when(context.createSocket(SocketType.ROUTER)).thenReturn(socket);
		assertThat(documentServerModule.routerSocket(context, SERVER_ADDRESS)).isEqualTo(socket);
		verify(socket).bind(SERVER_ADDRESS);
	}

	@Test
	void dealerSocket() {
		var context = mock(ZContext.class);
		var socket = mock(ZMQ.Socket.class);
		when(context.createSocket(SocketType.DEALER)).thenReturn(socket);
		assertThat(documentServerModule.dealerSocket(context)).isEqualTo(socket);
		verify(socket).bind(ServerConstants.WORKERS_ZMQ_ADDRESS);
	}
}
