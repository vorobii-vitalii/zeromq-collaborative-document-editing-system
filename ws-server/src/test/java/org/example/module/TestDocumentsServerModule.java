package org.example.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

class TestDocumentsServerModule {
	private static final String DOCUMENT_SERVER_URL = "tcp://*:31252";
	private static final int MIN_POOL_SIZE = 100;
	private static final int MAX_POOL_SIZE = 200;

	DocumentsServerModule documentsServerModule = new DocumentsServerModule();

	@Test
	void documentServerConnectionPool() throws Exception {
		var context = mock(ZContext.class);
		var socket = mock(ZMQ.Socket.class);
		when(context.createSocket(SocketType.REQ)).thenReturn(socket);
		var pool = documentsServerModule.documentServerConnectionPool(context, DOCUMENT_SERVER_URL, MIN_POOL_SIZE, MAX_POOL_SIZE);
		assertThat(pool.borrowObject()).isEqualTo(socket);
	}

	@SuppressWarnings("unchecked")
	@Test
	void documentServerCaller() {
		GenericObjectPool<ZMQ.Socket> documentServerConnectionPool = mock(GenericObjectPool.class);
		assertThat(documentsServerModule.documentServerCaller(documentServerConnectionPool)).isNotNull();
	}
}
