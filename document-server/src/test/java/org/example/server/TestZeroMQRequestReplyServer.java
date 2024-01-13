package org.example.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.example.request_handling.RequestHandler;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import com.google.flatbuffers.FlatBufferBuilder;

import document.editing.Request;
import document.editing.RequestHolder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;

class TestZeroMQRequestReplyServer {
	ServerCondition serverCondition = mock(ServerCondition.class);
	RequestHandler getRequestHandler = createMockRequestHandler(Request.GetRequest);
	RequestHandler changeRequestHandler = createMockRequestHandler(Request.ChangeRequest);

	Counter countRequests = mock(Counter.class);
	Timer requestProcessingTimer = mock(Timer.class);
	SocketMessageReader socketMessageReader = mock(SocketMessageReader.class);
	ZMQ.Socket replySocket = mock(ZMQ.Socket.class);
	DistributionSummary requestBytesDistribution = mock(DistributionSummary.class);

	ZeroMQRequestReplyServer server = new ZeroMQRequestReplyServer(
			serverCondition,
			socketMessageReader,
			Set.of(getRequestHandler, changeRequestHandler),
			countRequests,
			requestProcessingTimer,
			replySocket,
			requestBytesDistribution
	);

	@Test
	void givenGetRequest() {
		when(serverCondition.shouldProcessRequests()).thenReturn(true, false);
		var request = createDummyRequest(Request.GetRequest);
		when(socketMessageReader.read(replySocket)).thenReturn(request);
		doAnswer((Answer<?>) invocation -> {
			Runnable runnable = invocation.getArgument(0);
			runnable.run();
			return null;
		}).when(requestProcessingTimer).record(any(Runnable.class));
		server.startServer();
		verify(getRequestHandler).handleRequest(
				any(),
				eq(replySocket),
				eq(request)
		);
		verify(countRequests).increment();
	}

	@Test
	void givenChangeRequest() {
		when(serverCondition.shouldProcessRequests()).thenReturn(true, false);
		var request = createDummyRequest(Request.ChangeRequest);
		when(socketMessageReader.read(replySocket)).thenReturn(request);
		doAnswer((Answer<?>) invocation -> {
			Runnable runnable = invocation.getArgument(0);
			runnable.run();
			return null;
		}).when(requestProcessingTimer).record(any(Runnable.class));
		server.startServer();
		verify(changeRequestHandler).handleRequest(
				any(),
				eq(replySocket),
				eq(request)
		);
		verify(countRequests).increment();
	}

	@Test
	void givenRequestWithUnknownType() {
		when(serverCondition.shouldProcessRequests()).thenReturn(true, false);
		var request = createDummyRequest(Request.NONE);
		when(socketMessageReader.read(replySocket)).thenReturn(request);
		doAnswer((Answer<?>) invocation -> {
			Runnable runnable = invocation.getArgument(0);
			runnable.run();
			return null;
		}).when(requestProcessingTimer).record(any(Runnable.class));
		server.startServer();
		verify(countRequests).increment();
		verify(request).send(replySocket);
	}

	private ZMsg createDummyRequest(byte requestType) {
		ZMsg msg = mock(ZMsg.class);
		ZFrame frame = mock(ZFrame.class);

		var builder = new FlatBufferBuilder(500);
		RequestHolder.startRequestHolder(builder);
		RequestHolder.addRequestType(builder, requestType);
		builder.finish(RequestHolder.endRequestHolder(builder));

		var arr = builder.sizedByteArray();

		when(msg.contentSize()).thenReturn((long) arr.length);
		when(msg.getLast()).thenReturn(frame);
		when(frame.getData()).thenReturn(arr);

		return msg;
	}

	private RequestHandler createMockRequestHandler(byte requestType) {
		RequestHandler requestHandler = mock(RequestHandler.class);
		when(requestHandler.getHandledType()).thenReturn(requestType);
		return requestHandler;
	}

}
