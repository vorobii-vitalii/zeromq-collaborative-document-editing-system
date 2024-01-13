package org.example.server.impl;

import java.util.function.BiFunction;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.example.server.ServiceCaller;
import org.zeromq.ZMQ;

import reactor.core.publisher.Flux;

public class PooledReactiveZMQServiceCaller<T> implements ServiceCaller<T> {
	private final GenericObjectPool<ZMQ.Socket> socketPool;
	private final BiFunction<ZMQ.Socket, byte[], Flux<T>> caller;

	public PooledReactiveZMQServiceCaller(GenericObjectPool<ZMQ.Socket> socketPool, BiFunction<ZMQ.Socket, byte[], Flux<T>> caller) {
		this.socketPool = socketPool;
		this.caller = caller;
	}

	public Flux<T> callService(byte[] bytes) {
		return Flux.using(socketPool::borrowObject, v -> caller.apply(v, bytes), socketPool::returnObject);
	}

}
