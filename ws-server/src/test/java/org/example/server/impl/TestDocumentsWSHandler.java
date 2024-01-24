package org.example.server.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.server.ClientConnectedEventHandler;
import org.example.server.ClientDisconnectedEventHandler;
import org.example.server.ServiceCaller;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;
import reactor.test.StepVerifier;

@SuppressWarnings("unchecked")
class TestDocumentsWSHandler {
	private static final String DOCUMENT_ID_PARAM = "documentId";
	private static final int DOC_ID = 123;
	private static final byte[] REQUEST_PAYLOAD = {1, 2, 3};
	private static final byte[] RESPONSE_1 = {1, 2, 3, 5, 1};
	private static final byte[] RESPONSE_2 = {2, 10, 25};

	ClientConnectedEventHandler clientConnectedEventHandler = mock(ClientConnectedEventHandler.class);
	ClientDisconnectedEventHandler clientDisconnectedEventHandler = mock(ClientDisconnectedEventHandler.class);
	ServiceCaller<byte[]> serviceCaller = mock(ServiceCaller.class);

	DocumentsWSHandler documentsWSHandler = new DocumentsWSHandler(
			clientConnectedEventHandler,
			clientDisconnectedEventHandler,
			serviceCaller,
			Schedulers.immediate()
	);

	TestWSInbound wsInbound = mock(TestWSInbound.class);
	WebsocketOutbound websocketOutbound = mock(WebsocketOutbound.class);

	@Test
	void path() {
		assertThat(documentsWSHandler.path()).isEqualTo("/documents/{documentId}");
	}

	@Test
	void handlerExpectMessagesAreAggregatedAndSendToDocumentServerAndAllRepliesAreReplicatedToClient() {
		ArgumentCaptor<Publisher<ByteBuf>> buffers = ArgumentCaptor.forClass(Publisher.class);

		var webSocketFrame = mock(WebSocketFrame.class);
		when(webSocketFrame.content()).thenReturn(Unpooled.wrappedBuffer(REQUEST_PAYLOAD));

		when(wsInbound.param(DOCUMENT_ID_PARAM)).thenReturn(String.valueOf(DOC_ID));
		when(wsInbound.aggregateFrames()).thenReturn(wsInbound);
		when(wsInbound.receiveCloseStatus()).thenReturn(Mono.empty());
		when(wsInbound.receiveFrames()).thenReturn(Flux.just(webSocketFrame));
		when(serviceCaller.callService(REQUEST_PAYLOAD)).thenReturn(Flux.just(RESPONSE_1, RESPONSE_2));
		when(websocketOutbound.send(any())).thenReturn(websocketOutbound);

		documentsWSHandler.handler().apply(wsInbound, websocketOutbound).subscribe(noBackPressureSubscriber());

		verify(websocketOutbound, timeout(200)).send(buffers.capture());
		StepVerifier.create(buffers.getValue())
				.expectNext(Unpooled.wrappedBuffer(RESPONSE_1))
				.expectNext(Unpooled.wrappedBuffer(RESPONSE_2))
				.expectComplete()
				.log()
				.verify();
	}

	private CoreSubscriber<Void> noBackPressureSubscriber() {
		return new CoreSubscriber<>() {
			@Override
			public void onSubscribe(Subscription subscription) {
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(Void unused) {

			}

			@Override
			public void onError(Throwable t) {

			}

			@Override
			public void onComplete() {

			}
		};
	}

	interface TestWSInbound extends WebsocketInbound, HttpServerRequest {
	}

}
