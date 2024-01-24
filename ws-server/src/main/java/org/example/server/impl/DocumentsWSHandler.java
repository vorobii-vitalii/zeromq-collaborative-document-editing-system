package org.example.server.impl;

import java.util.Objects;
import java.util.function.BiFunction;

import org.example.server.ClientConnectedEventHandler;
import org.example.server.ClientDisconnectedEventHandler;
import org.example.server.WSHandler;
import org.example.server.ServiceCaller;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

public class DocumentsWSHandler implements WSHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsWSHandler.class);
	private static final String DOCUMENT_ID_PARAM = "documentId";

	private final ClientConnectedEventHandler clientConnectedEventHandler;
	private final ClientDisconnectedEventHandler clientDisconnectedEventHandler;
	private final ServiceCaller<byte[]> serviceCaller;
	private final Scheduler subscribeScheduler;

	public DocumentsWSHandler(
			ClientConnectedEventHandler clientConnectedEventHandler,
			ClientDisconnectedEventHandler clientDisconnectedEventHandler,
			ServiceCaller<byte[]> serviceCaller,
			Scheduler subscribeScheduler
	) {
		this.clientConnectedEventHandler = clientConnectedEventHandler;
		this.clientDisconnectedEventHandler = clientDisconnectedEventHandler;
		this.serviceCaller = serviceCaller;
		this.subscribeScheduler = subscribeScheduler;
	}

	@Override
	public String path() {
		return "/documents/{" + DOCUMENT_ID_PARAM + "}";
	}

	@Override
	public BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> handler() {
		return (wsInbound, wsOutbound) -> {
			var httpServerOperations = (HttpServerRequest) wsInbound;

			var documentId = Integer.parseInt(Objects.requireNonNull(httpServerOperations.param(DOCUMENT_ID_PARAM)));

			clientConnectedEventHandler.onClientConnect(documentId, wsOutbound);

			wsInbound.receiveCloseStatus()
					.subscribeOn(Schedulers.parallel())
					.subscribe(closeStatus -> {
						LOGGER.info("Connection closed with code = {} reason = {}", closeStatus.code(),
								closeStatus.reasonText());
						clientDisconnectedEventHandler.onClientDisconnect(documentId, wsOutbound);
					});
			return wsInbound
					.aggregateFrames()
					.receiveFrames()
					.doOnNext(v -> LOGGER.info("New WS message of length {}", v.content().readableBytes()))
					.map(v -> v.content().retain())
					.map(this::toByteArray)
					.publishOn(Schedulers.boundedElastic())
					.map(v -> serviceCaller.callService(v).map(Unpooled::wrappedBuffer))
					.flatMap(wsOutbound::send)
					.subscribeOn(subscribeScheduler);
		};
	}

	private byte[] toByteArray(ByteBuf byteBuf) {
		if (byteBuf.hasArray()) {
			return byteBuf.array();
		} else {
			return ByteBufUtil.getBytes(byteBuf, 0, byteBuf.readableBytes(), true);
		}
	}
}
