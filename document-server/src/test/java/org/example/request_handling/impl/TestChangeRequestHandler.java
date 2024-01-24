package org.example.request_handling.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.constants.DocumentElementSchema.CHAR_ID;
import static org.example.constants.DocumentElementSchema.DISAMBIGUATOR;
import static org.example.constants.DocumentElementSchema.DOCUMENT_ID;
import static org.example.constants.DocumentElementSchema.IS_RIGHT;
import static org.example.constants.DocumentElementSchema.PARENT_CHAR_ID;
import static org.example.constants.DocumentElementSchema.VALUE;
import static org.example.request_handling.impl.ChangeRequestHandler.SET;
import static org.example.request_handling.impl.ChangeRequestHandler.UPSERT_UPDATE_OPTIONS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import com.google.flatbuffers.FlatBufferBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;

import document.editing.Change;
import document.editing.ChangeRequest;
import document.editing.Request;
import document.editing.RequestHolder;
import io.micrometer.core.instrument.Counter;

@SuppressWarnings("unchecked")
class TestChangeRequestHandler {

	MongoCollection<Document> documentsCollection = mock(MongoCollection.class);

	Counter totalChangesCount = mock(Counter.class);

	ZMQ.Socket socket = mock(ZMQ.Socket.class);

	ZMsg msg = mock(ZMsg.class);

	ChangeRequestHandler changeRequestHandler = new ChangeRequestHandler(documentsCollection, totalChangesCount);

	public static Object[][] makeChangeRequestTestParameters() {
		return new Object[][] {
				{
						createChangeRequest("2", "1", 'A', false, 2, 5),
						List.of(
								new UpdateOneModel<>(
										new Document()
												.append(DOCUMENT_ID, 5)
												.append(PARENT_CHAR_ID, "1")
												.append(IS_RIGHT, false)
												.append(DISAMBIGUATOR, 2),
										new Document().append(
												SET,
												new Document().append(VALUE, (int) 'A').append(CHAR_ID, "2")
										),
										UPSERT_UPDATE_OPTIONS
								)
						)
				},
				{
						createChangeRequest("2", null, 'A', false, 2, 5),
						List.of(
								new UpdateOneModel<>(
										new Document()
												.append(DOCUMENT_ID, 5)
												.append(PARENT_CHAR_ID, null)
												.append(IS_RIGHT, false)
												.append(DISAMBIGUATOR, 2),
										new Document().append(
												SET,
												new Document().append(VALUE, (int) 'A').append(CHAR_ID, "2")
										),
										UPSERT_UPDATE_OPTIONS
								)
						)
				},
				{
						createChangeRequest("2", "1", 'A', true, 2, 5),
						List.of(
								new UpdateOneModel<>(
										new Document()
												.append(DOCUMENT_ID, 5)
												.append(PARENT_CHAR_ID, "1")
												.append(IS_RIGHT, true)
												.append(DISAMBIGUATOR, 2),
										new Document().append(
												SET,
												new Document().append(VALUE, (int) 'A').append(CHAR_ID, "2")
										),
										UPSERT_UPDATE_OPTIONS
								)
						)
				},
				{
						createChangeRequest("2", "1", 0, true, 2, 5),
						List.of(
								new UpdateOneModel<>(
										Filters.and(
												Filters.eq(DOCUMENT_ID, 5),
												Filters.eq(CHAR_ID, "2")
										),
										new Document().append(SET, new Document(VALUE, null))
								))
				}
		};
	}

	private static FlatBufferBuilder createChangeRequest(
			String charId,
			String parentCharId,
			int character,
			boolean isRight,
			int disambiguator,
			int docId
	) {
		var flatBufferBuilder = new FlatBufferBuilder();

		int charIdOffset = flatBufferBuilder.createString(charId);
		int parentCharIdOffset = parentCharId == null ? 0 : flatBufferBuilder.createString(parentCharId);

		int changeOffset = Change.createChange(flatBufferBuilder, charIdOffset, parentCharIdOffset, isRight, disambiguator, character);

		int changeRequestOffset = ChangeRequest.createChangeRequest(
				flatBufferBuilder,
				docId,
				ChangeRequest.createChangesVector(flatBufferBuilder, new int[] {changeOffset}));

		RequestHolder.startRequestHolder(flatBufferBuilder);
		RequestHolder.addRequest(flatBufferBuilder, Request.ChangeRequest);
		RequestHolder.addRequest(flatBufferBuilder, changeRequestOffset);
		flatBufferBuilder.finish(RequestHolder.endRequestHolder(flatBufferBuilder));
		return flatBufferBuilder;
	}

	@Test
	void getHandledType() {
		assertThat(changeRequestHandler.getHandledType()).isEqualTo(Request.ChangeRequest);
	}

	@ParameterizedTest
	@MethodSource("makeChangeRequestTestParameters")
	void handleRequest(FlatBufferBuilder flatBufferBuilder, List<UpdateOneModel<Document>> expectedUpdates) {
		var frame = mock(ZFrame.class);
		when(msg.getLast()).thenReturn(frame);

		changeRequestHandler.handleRequest(RequestHolder.getRootAsRequestHolder(flatBufferBuilder.dataBuffer()), socket, msg);

		ArgumentCaptor<List<UpdateOneModel<Document>>> captor = ArgumentCaptor.forClass(List.class);

		verify(documentsCollection).bulkWrite(captor.capture());
		assertThat(captor.getValue()).usingRecursiveFieldByFieldElementComparator().isEqualTo(expectedUpdates);

		verify(totalChangesCount).increment(1);
		verify(frame).reset(any(byte[].class));
		verify(msg).send(socket);
	}

}
