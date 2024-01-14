package org.example.server.impl;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.zeromq.ZMQ;

import reactor.test.StepVerifier;

class TestDocumentServiceCaller {
	private static final byte[] REQUEST = new byte[] {1, 2, 3, 4, 5};
	public static final byte[] RESPONSE_2 = {3, 4};
	public static final byte[] RESPONSE_1 = {1, 2};

	ZMQ.Socket socket = mock(ZMQ.Socket.class);

	DocumentServiceCaller documentServiceCaller = new DocumentServiceCaller();

	@Test
	void callGivenHappyPath() {
		when(socket.recv()).thenReturn(RESPONSE_1, RESPONSE_2);
		when(socket.hasReceiveMore()).thenReturn(true, false);
		StepVerifier.create(documentServiceCaller.apply(socket, REQUEST))
				.expectNext(RESPONSE_1)
				.expectNext(RESPONSE_2)
				.expectComplete()
				.log()
				.verify();
		InOrder inOrder = Mockito.inOrder(socket);
		inOrder.verify(socket).send(REQUEST);
		inOrder.verify(socket, atLeastOnce()).recv();
	}

	@Test
	void callGivenErrorOccurredWhenReadingResponse() {
		when(socket.recv()).thenThrow(new RuntimeException());
		StepVerifier.create(documentServiceCaller.apply(socket, REQUEST))
				.expectError()
				.log()
				.verify();
		InOrder inOrder = Mockito.inOrder(socket);
		inOrder.verify(socket).send(REQUEST);
		inOrder.verify(socket, atLeastOnce()).recv();
	}

}
