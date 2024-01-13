package org.example.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.example.server.Deserializer;
import org.example.server.ServerCondition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.zeromq.ZMQ;

import com.google.flatbuffers.FlatBufferBuilder;

import document.editing.DocumentElement;
import document.editing.DocumentUpdatedEvent;
import document.editing.Response;
import document.editing.ResponseHolder;
import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.netty.http.websocket.WebsocketOutbound;
import reactor.test.StepVerifier;

@SuppressWarnings("unchecked")
class TestDocumentEventsReadProcess {
	private static final int DOC_ID_NO_SUBSCRIBERS = 1;
	private static final int DOC_ID_SINGLE_SUBSCRIBER = 2;
	private static final int DOC_ID_MULTI_SUBSCRIBERS = 3;
	public static final byte[] BYTES = {1, 2, 3};

	WebsocketOutbound CONNECTION_1 = mock(WebsocketOutbound.class);
	WebsocketOutbound CONNECTION_2 = mock(WebsocketOutbound.class);
	WebsocketOutbound CONNECTION_3 = mock(WebsocketOutbound.class);

	ZMQ.Socket socket = mock(ZMQ.Socket.class);
	ServerCondition serverCondition = mock(ServerCondition.class);
	Deserializer<DocumentUpdatedEvent> documentUpdatedEventDeserializer = mock(Deserializer.class);

	Map<Integer, List<WebsocketOutbound>> connectionsByDocumentId = Map.of(
			DOC_ID_SINGLE_SUBSCRIBER, List.of(CONNECTION_1),
			DOC_ID_MULTI_SUBSCRIBERS, List.of(CONNECTION_2, CONNECTION_3)
	);

	DocumentEventsReadProcess readProcess = new DocumentEventsReadProcess(
			socket,
			connectionsByDocumentId,
			serverCondition,
			documentUpdatedEventDeserializer
	);

	@Test
	void eventsReadProcessNullMsgReturned() {
		when(serverCondition.shouldContinue()).thenReturn(true, false);
		when(socket.recv()).thenReturn(null);
		readProcess.run();
		verifyNoInteractions(documentUpdatedEventDeserializer);
	}

	@Test
	void eventsReadProcessDocumentedNoOneIsSubscribedToWasUpdated() {
		when(serverCondition.shouldContinue()).thenReturn(true, false);
		when(socket.recv()).thenReturn(BYTES);
		when(documentUpdatedEventDeserializer.deserialize(BYTES)).thenReturn(
				documentUpdatedEvent(
						DOC_ID_NO_SUBSCRIBERS,
						"2",
						"1",
						12,
						false,
						17
				)
		);
		readProcess.run();
		verifyNoInteractions(CONNECTION_1, CONNECTION_2, CONNECTION_3);
	}

	@Test
	void eventsReadProcessDocumentedSingleSubscriber() {
		ArgumentCaptor<Flux<ByteBuf>> buffersCaptor = ArgumentCaptor.forClass(Flux.class);

		when(serverCondition.shouldContinue()).thenReturn(true, false);
		when(socket.recv()).thenReturn(BYTES);
		when(documentUpdatedEventDeserializer.deserialize(BYTES)).thenReturn(
				documentUpdatedEvent(
						DOC_ID_SINGLE_SUBSCRIBER,
						"2",
						"1",
						12,
						false,
						17
				)
		);
		when(CONNECTION_1.send(any())).thenReturn(CONNECTION_1);
		readProcess.run();
		verify(CONNECTION_1).send(buffersCaptor.capture());
		StepVerifier.create(buffersCaptor.getValue())
				.assertNext(buf -> assertThat(buf.array()).containsExactly(serializeDocumentElement(
						"2",
						"1",
						12,
						false,
						17
				).sizedByteArray()))
				.expectComplete()
				.log()
				.verify();
	}

	@Test
	void eventsReadProcessDocumentedMutlipleSubscribers() {
		ArgumentCaptor<Flux<ByteBuf>> buffersCaptor1 = ArgumentCaptor.forClass(Flux.class);
		ArgumentCaptor<Flux<ByteBuf>> buffersCaptor2 = ArgumentCaptor.forClass(Flux.class);

		when(serverCondition.shouldContinue()).thenReturn(true, false);
		when(socket.recv()).thenReturn(BYTES);
		when(documentUpdatedEventDeserializer.deserialize(BYTES)).thenReturn(
				documentUpdatedEvent(
						DOC_ID_MULTI_SUBSCRIBERS,
						"3",
						"2",
						20,
						true,
						11
				)
		);
		when(CONNECTION_2.send(any())).thenReturn(CONNECTION_2);
		when(CONNECTION_3.send(any())).thenReturn(CONNECTION_3);
		readProcess.run();
		verify(CONNECTION_2).send(buffersCaptor1.capture());
		verify(CONNECTION_3).send(buffersCaptor2.capture());
		StepVerifier.create(buffersCaptor1.getValue())
				.assertNext(buf -> assertThat(buf.array()).containsExactly(serializeDocumentElement(
						"3",
						"2",
						20,
						true,
						11
				).sizedByteArray()))
				.expectComplete()
				.log()
				.verify();
		StepVerifier.create(buffersCaptor2.getValue())
				.assertNext(buf -> assertThat(buf.array()).containsExactly(serializeDocumentElement(
						"3",
						"2",
						20,
						true,
						11
				).sizedByteArray()))
				.expectComplete()
				.log()
				.verify();
	}

	private FlatBufferBuilder serializeDocumentElement(
			String charId,
			String parentCharId,
			int character,
			boolean isRight,
			int disambiguator
	) {
		var builder = new FlatBufferBuilder();
		var charIdOffset = builder.createString(charId);
		var parentIdOffset = parentCharId != null ? builder.createString(parentCharId) : 0;
		var documentElementOffset = DocumentElement.createDocumentElement(
				builder,
				charIdOffset,
				parentIdOffset,
				isRight,
				disambiguator,
				character
		);
		ResponseHolder.finishResponseHolderBuffer(
				builder,
				ResponseHolder.createResponseHolder(builder, Response.DocumentElement, documentElementOffset));
		return builder;
	}

	private DocumentUpdatedEvent documentUpdatedEvent(
			int docId,
			String charId,
			String parentCharId,
			int character,
			boolean isRight,
			int disambiguator
	) {
		var flatBufferBuilder = new FlatBufferBuilder();
		int parentCharIdOffset = parentCharId != null ? flatBufferBuilder.createString(parentCharId) : 0;
		int charIdOffset = flatBufferBuilder.createString(charId);
		int offset = DocumentUpdatedEvent.createDocumentUpdatedEvent(
				flatBufferBuilder, charIdOffset, parentCharIdOffset, isRight, disambiguator, character, docId);
		flatBufferBuilder.finish(offset);
		return DocumentUpdatedEvent.getRootAsDocumentUpdatedEvent(flatBufferBuilder.dataBuffer());
	}

}
