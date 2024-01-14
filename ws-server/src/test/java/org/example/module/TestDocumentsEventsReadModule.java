package org.example.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

class TestDocumentsEventsReadModule {

	public static final String DOCUMENT_EVENT_SERVER_URL = "tcp://host:31225";
	DocumentsEventsReadModule documentsEventsReadModule = new DocumentsEventsReadModule();

	@Test
	void documentEventServerSubscribeSocket() {
		var context = mock(ZContext.class);
		var socket = mock(ZMQ.Socket.class);
		when(context.createSocket(SocketType.SUB)).thenReturn(socket);
		assertThat(documentsEventsReadModule.documentEventServerSubscribeSocket(context, DOCUMENT_EVENT_SERVER_URL))
				.isEqualTo(socket);
		verify(socket).connect(DOCUMENT_EVENT_SERVER_URL);
		verify(socket).subscribe(ZMQ.SUBSCRIPTION_ALL);
	}

	@Test
	void subscribersByDocumentId() {
		assertThat(documentsEventsReadModule.subscribersByDocumentId()).isEmpty();
	}
}
