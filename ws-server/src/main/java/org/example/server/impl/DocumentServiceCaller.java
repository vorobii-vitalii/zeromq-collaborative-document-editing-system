package org.example.server.impl;

import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import reactor.core.publisher.Flux;

public class DocumentServiceCaller implements BiFunction<ZMQ.Socket, byte[], Flux<byte[]>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentServiceCaller.class);

	@Override
	public Flux<byte[]> apply(ZMQ.Socket socket, byte[] bytes) {
		return Flux.create(sink -> {
			try {
				LOGGER.info("Sending message to socket of length = {}", bytes.length);
				socket.send(bytes);
				LOGGER.info("Request sent...");
				do {
					var v = socket.recv();
					if (v == null) {
						LOGGER.warn("For some reason got null response...");
						break;
					}
					sink.next(v);
					LOGGER.info("Receive and written response of length = {}", v.length);
				}
				while (socket.hasReceiveMore());
				sink.complete();
			}
			catch (Exception error) {
				LOGGER.error("Error occurred on document service call", error);
				sink.error(error);
			}
		});
	}
}
