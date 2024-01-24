package org.example.server.impl;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.example.server.ServiceCaller;
import org.zeromq.ZMQ;

import reactor.core.publisher.Flux;

public class ZMQServiceCaller<T> implements ServiceCaller<T> {
	private final Supplier<ZMQ.Socket> socketCreator;
	private final BiFunction<ZMQ.Socket, byte[], Flux<T>> caller;

	public ZMQServiceCaller(Supplier<ZMQ.Socket> socketCreator, BiFunction<ZMQ.Socket, byte[], Flux<T>> caller) {
		this.socketCreator = socketCreator;
		this.caller = caller;
	}

	public Flux<T> callService(byte[] bytes) {
		return caller.apply(socketCreator.get(), bytes);
	}

}
