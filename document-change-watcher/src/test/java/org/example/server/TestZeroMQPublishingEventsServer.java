package org.example.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.example.serialization.Serializer;
import org.junit.jupiter.api.Test;
import org.zeromq.ZMQ;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;

@SuppressWarnings("unchecked")
class TestZeroMQPublishingEventsServer {

	Serializer<Integer> serializer = mock(Serializer.class);

	Supplier<Iterator<Integer>> eventsSupplier = mock(Supplier.class);

	ZMQ.Socket socket = mock(ZMQ.Socket.class);

	Counter eventsCounter = mock(Counter.class);

	DistributionSummary bytesWritten = mock(DistributionSummary.class);

	ZeroMQPublishingEventsServer<Integer> zeroMQPublishingEventsServer = new ZeroMQPublishingEventsServer<>(
			serializer,
			eventsSupplier,
			socket,
			eventsCounter,
			bytesWritten
	);

	@Test
	void startPublishingServerGivenAllEventsCanBeSerialized() {
		Iterator<Integer> eventsIterator = createMockIterator(1, 2, 3, 4, 5);
		when(eventsSupplier.get()).thenReturn(eventsIterator);
		when(serializer.serialize(anyInt())).thenAnswer(invocation -> {
			int v = invocation.getArgument(0);
			return Optional.of(new byte[] {(byte) v});
		});
		zeroMQPublishingEventsServer.startPublishingServer();
		verify(eventsCounter, times(5)).increment();
		verify(bytesWritten, times(5)).record(1);
		verify(socket).send(new byte[] {1});
		verify(socket).send(new byte[] {2});
		verify(socket).send(new byte[] {3});
		verify(socket).send(new byte[] {4});
		verify(socket).send(new byte[] {5});
	}

	@Test
	void startPublishingServerGivenSomeEventsCannotBeSerialized() {
		Iterator<Integer> eventsIterator = createMockIterator(1);
		when(eventsSupplier.get()).thenReturn(eventsIterator);
		when(serializer.serialize(anyInt())).thenAnswer(invocation -> Optional.empty());
		zeroMQPublishingEventsServer.startPublishingServer();
		verify(eventsCounter).increment();
		verify(socket, never()).send(any(byte[].class));
	}

	@SafeVarargs
	private <T> Iterator<T> createMockIterator(T... arr) {
		Iterator<T> iterator = mock(Iterator.class);
		doAnswer(invocation -> {
			Consumer<T> consumer = invocation.getArgument(0);
			for (T t : arr) {
				consumer.accept(t);
			}
			return null;
		}).when(iterator).forEachRemaining(any());
		return iterator;
	}


}