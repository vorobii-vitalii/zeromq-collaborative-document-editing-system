package org.example.request_handling.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.constants.DocumentElementSchema.CHAR_ID;
import static org.example.constants.DocumentElementSchema.DISAMBIGUATOR;
import static org.example.constants.DocumentElementSchema.DOCUMENT_ID;
import static org.example.constants.DocumentElementSchema.IS_RIGHT;
import static org.example.constants.DocumentElementSchema.PARENT_CHAR_ID;
import static org.example.constants.DocumentElementSchema.VALUE;
import static org.example.request_handling.impl.GetRequestHandler.READ_DOCUMENT_PROJECTION;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import com.google.flatbuffers.FlatBufferBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

import document.editing.DocumentElement;
import document.editing.GetRequest;
import document.editing.Request;
import document.editing.RequestHolder;
import document.editing.Response;
import document.editing.ResponseHolder;
import io.micrometer.core.instrument.DistributionSummary;

@SuppressWarnings("unchecked")
class TestGetRequestHandler {

	public static final int DOCUMENT = 90;
	MongoCollection<Document> documentsCollection = mock(MongoCollection.class);
	DistributionSummary counterBytesWritten = mock(DistributionSummary.class);
	DistributionSummary counterDocumentElementsWritten = mock(DistributionSummary.class);

	GetRequestHandler getRequestHandler =
			new GetRequestHandler(documentsCollection, counterBytesWritten, counterDocumentElementsWritten);

	FindIterable<Document> findIterable = mock(FindIterable.class);

	MongoCursor<Document> mongoCursor = mock(MongoCursor.class);

	ZMQ.Socket socket = mock(ZMQ.Socket.class);

	ZMsg msg = mock(ZMsg.class);

	public static Object[][] getDocumentElementsTestParameters() {
		return new Object[][] {
				{
						List.of(
								new Document()
										.append(CHAR_ID, "1")
										.append(DOCUMENT_ID, 5)
										.append(PARENT_CHAR_ID, null)
										.append(IS_RIGHT, false)
										.append(DISAMBIGUATOR, 2)
										.append(VALUE, 5),
								new Document()
										.append(CHAR_ID, "2")
										.append(DOCUMENT_ID, 5)
										.append(PARENT_CHAR_ID, "1")
										.append(IS_RIGHT, false)
										.append(DISAMBIGUATOR, 2)
										.append(VALUE, 6)
						),
						List.of(
								createDocumentElement("1", null, 5, false, 2),
								createDocumentElement("2", "1", 6, false, 2)
						)
				},
				{
						List.of(
								new Document()
										.append(CHAR_ID, "1")
										.append(DOCUMENT_ID, 5)
										.append(PARENT_CHAR_ID, null)
										.append(IS_RIGHT, false)
										.append(DISAMBIGUATOR, 2)
										.append(VALUE, 5),
								new Document()
										.append(CHAR_ID, "3")
										.append(DOCUMENT_ID, 5)
										.append(PARENT_CHAR_ID, "1")
										.append(IS_RIGHT, true)
										.append(DISAMBIGUATOR, 4)
										.append(VALUE, 6)
						),
						List.of(
								createDocumentElement("1", null, 5, false, 2),
								createDocumentElement("3", "1", 6, true, 4)
						)
				},
				{
						List.of(
								new Document()
										.append(CHAR_ID, "1")
										.append(DOCUMENT_ID, 5)
										.append(PARENT_CHAR_ID, null)
										.append(IS_RIGHT, false)
										.append(DISAMBIGUATOR, 2)
										.append(VALUE, 5),
								new Document()
										.append(CHAR_ID, "3")
										.append(DOCUMENT_ID, 5)
										.append(PARENT_CHAR_ID, "1")
										.append(IS_RIGHT, true)
										.append(DISAMBIGUATOR, 4)
						),
						List.of(
								createDocumentElement("1", null, 5, false, 2),
								createDocumentElement("3", "1", 0, true, 4)
						)
				}
		};
	}

	private static FlatBufferBuilder createDocumentElement(
			String charId,
			String parentCharId,
			int character,
			boolean isRight,
			int disambiguator
	) {
		var builder = new FlatBufferBuilder();
		var charIdOffset = builder.createString(charId);
		var parentIdOffset = parentCharId != null ? builder.createString(parentCharId) : 0;
		DocumentElement.startDocumentElement(builder);
		DocumentElement.addCharId(builder, charIdOffset);
		DocumentElement.addParentCharId(builder, parentIdOffset);
		DocumentElement.addIsRight(builder, isRight);
		DocumentElement.addDisambiguator(builder, disambiguator);
		DocumentElement.addCharacter(builder, character);
		var documentElementOffset = DocumentElement.endDocumentElement(builder);
		ResponseHolder.startResponseHolder(builder);
		ResponseHolder.addResponseType(builder, Response.DocumentElement);
		ResponseHolder.addResponse(builder, documentElementOffset);
		ResponseHolder.finishResponseHolderBuffer(builder, ResponseHolder.endResponseHolder(builder));
		return builder;
	}

	@Test
	void getHandledType() {
		assertThat(getRequestHandler.getHandledType()).isEqualTo(Request.GetRequest);
	}

	@ParameterizedTest
	@MethodSource("getDocumentElementsTestParameters")
	void handleRequestGivenDocumentNotEmpty(List<Document> mongoDocuments, List<FlatBufferBuilder> flatBufferBuilders) {
		var frame = mock(ZFrame.class);

		when(documentsCollection.find(Filters.eq(DOCUMENT_ID, DOCUMENT))).thenReturn(findIterable);
		when(findIterable.batchSize(anyInt())).thenReturn(findIterable);
		when(findIterable.projection(READ_DOCUMENT_PROJECTION)).thenReturn(findIterable);
		when(findIterable.cursor()).thenReturn(mongoCursor);

		when(msg.getLast()).thenReturn(frame);

		doAnswer(invocation -> {
			Consumer<Document> documentConsumer = invocation.getArgument(0);
			for (Document mongoDocument : mongoDocuments) {
				documentConsumer.accept(mongoDocument);
			}
			return null;
		}).when(mongoCursor).forEachRemaining(any());

		getRequestHandler.handleRequest(createGetDocumentRequest(), socket, msg);

		var captor = ArgumentCaptor.forClass(byte[].class);
		var inOrder = Mockito.inOrder(msg, frame);
		inOrder.verify(frame).destroy();
		inOrder.verify(msg, atLeastOnce()).add(captor.capture());
		inOrder.verify(msg).send(socket);

		assertThat(captor.getAllValues())
				.containsExactlyElementsOf(flatBufferBuilders.stream().map(FlatBufferBuilder::sizedByteArray).collect(Collectors.toList()));
	}

	@Test
	void handleRequestGivenDocumentEmpty() {
		var frame = mock(ZFrame.class);

		when(documentsCollection.find(Filters.eq(DOCUMENT_ID, DOCUMENT))).thenReturn(findIterable);
		when(findIterable.batchSize(anyInt())).thenReturn(findIterable);
		when(findIterable.projection(READ_DOCUMENT_PROJECTION)).thenReturn(findIterable);
		when(findIterable.cursor()).thenReturn(mongoCursor);
		when(msg.getLast()).thenReturn(frame);

		getRequestHandler.handleRequest(createGetDocumentRequest(), socket, msg);

		var captor = ArgumentCaptor.forClass(byte[].class);
		var inOrder = Mockito.inOrder(msg, frame);
		inOrder.verify(frame).destroy();
		inOrder.verify(msg, atLeastOnce()).add(captor.capture());
		inOrder.verify(msg).send(socket);
	}

	private RequestHolder createGetDocumentRequest() {
		var flatBufferBuilder = new FlatBufferBuilder();
		int getRequestOffset = GetRequest.createGetRequest(flatBufferBuilder, DOCUMENT);
		flatBufferBuilder.finish(RequestHolder.createRequestHolder(flatBufferBuilder, Request.GetRequest, getRequestOffset));
		return RequestHolder.getRootAsRequestHolder(flatBufferBuilder.dataBuffer());
	}

}
