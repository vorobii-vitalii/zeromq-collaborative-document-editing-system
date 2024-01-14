package org.example.server.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.BiFunction;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.Test;
import org.zeromq.ZMQ;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SuppressWarnings("unchecked")
class TestPooledReactiveZMQServiceCaller {

	public static final byte[] REQUEST = {1, 2, 3, 4};
	GenericObjectPool<ZMQ.Socket> objectPool = mock(GenericObjectPool.class);

	BiFunction<ZMQ.Socket, byte[], Flux<Integer>> caller = mock(BiFunction.class);

	PooledReactiveZMQServiceCaller<Integer> pooledReactiveZMQServiceCaller =
			new PooledReactiveZMQServiceCaller<>(objectPool, caller);

	ZMQ.Socket socket = mock(ZMQ.Socket.class);

	@Test
	void callServiceHappyPath() throws Exception {
		when(objectPool.borrowObject()).thenReturn(socket);
		when(caller.apply(socket, REQUEST)).thenReturn(Flux.just(1, 2, 3));
		StepVerifier.create(pooledReactiveZMQServiceCaller.callService(REQUEST))
				.expectNext(1)
				.expectNext(2)
				.expectNext(3)
				.expectComplete()
				.log()
				.verify();
		// Verify socket was returned to pool...
		verify(objectPool).returnObject(socket);
	}

	@Test
	void callServiceGivenServiceCallFailed() throws Exception {
		when(objectPool.borrowObject()).thenReturn(socket);
		when(caller.apply(socket, REQUEST)).thenReturn(Flux.error(new NullPointerException()));
		StepVerifier.create(pooledReactiveZMQServiceCaller.callService(REQUEST))
				.expectError()
				.log()
				.verify();
		// Verify socket was returned to pool...
		verify(objectPool).returnObject(socket);
	}

}