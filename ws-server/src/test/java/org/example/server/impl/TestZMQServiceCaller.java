package org.example.server.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.zeromq.ZMQ;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SuppressWarnings("unchecked")
class TestZMQServiceCaller {

	public static final byte[] REQUEST = {1, 2, 3, 4};

	BiFunction<ZMQ.Socket, byte[], Flux<Integer>> caller = mock(BiFunction.class);

	ZMQ.Socket socket = mock(ZMQ.Socket.class);
	ZMQServiceCaller<Integer> ZMQServiceCaller =
			new ZMQServiceCaller<>(() -> socket, caller);

	@Test
	void callServiceHappyPath() {
		when(caller.apply(socket, REQUEST)).thenReturn(Flux.just(1, 2, 3));
		StepVerifier.create(ZMQServiceCaller.callService(REQUEST))
				.expectNext(1)
				.expectNext(2)
				.expectNext(3)
				.expectComplete()
				.log()
				.verify();
	}

	@Test
	void callServiceGivenServiceCallFailed() {
		when(caller.apply(socket, REQUEST)).thenReturn(Flux.error(new NullPointerException()));
		StepVerifier.create(ZMQServiceCaller.callService(REQUEST))
				.expectError()
				.log()
				.verify();
	}

}