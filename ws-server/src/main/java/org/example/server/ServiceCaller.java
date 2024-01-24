package org.example.server;

import reactor.core.publisher.Flux;

public interface ServiceCaller<T> {
	Flux<T> callService(byte[] arr);
}
