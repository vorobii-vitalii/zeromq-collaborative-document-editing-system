package org.example.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.example.server.ClientConnectedEventHandler;
import org.example.server.ClientDisconnectedEventHandler;
import org.example.server.WSHandler;
import org.example.server.impl.DocumentsWSHandler;
import org.example.server.ServiceCaller;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.websocket.WebsocketOutbound;
import reactor.netty.resources.LoopResources;

@Module(includes = {DocumentsServerModule.class, DocumentsEventsReadModule.class})
public class WebSocketServerModule {
	private static final String EVENT_LOOP_PREFIX = "event-loop";
	protected static final int IO_THREADS_DEFAULT = 1;
	protected static final int WORKER_THREADS_DEFAULT = 2;
	private static final boolean DAEMON = true;

	@Provides
	@IntoSet
	@Singleton
	WSHandler documentWSHandler(
			@Named("subscribersByDocumentId")
			ConcurrentHashMap<Integer, List<WebsocketOutbound>> subscribersByDocumentId,
			ServiceCaller<byte[]> documentServerCaller
	) {
		return new DocumentsWSHandler(
				getClientConnectedEventHandler(subscribersByDocumentId),
				getClientDisconnectedEventHandler(subscribersByDocumentId),
				documentServerCaller,
				Schedulers.boundedElastic()
		);
	}

	@Provides
	@Singleton
	HttpServer webSocketServer(
			Set<WSHandler> wsHandlers,
			@Named("port") int port
	) {
		return HttpServer.create()
				.runOn(LoopResources.create(EVENT_LOOP_PREFIX, getIoThreads(), getWorkerThreads(), DAEMON))
				.port(port)
				.noSSL()
				.route(routes -> wsHandlers.forEach(handler -> routes.ws(handler.path(), handler.handler())));
	}

	public ClientDisconnectedEventHandler getClientDisconnectedEventHandler(
			ConcurrentHashMap<Integer, List<WebsocketOutbound>> subscribersByDocumentId
	) {
		return (documentId, wsOutbound) -> subscribersByDocumentId.compute(documentId, (id, v) -> {
			if (v == null || v.isEmpty()) {
				return null;
			}
			v.remove(wsOutbound);
			return v;
		});
	}

	public ClientConnectedEventHandler getClientConnectedEventHandler(
			ConcurrentHashMap<Integer, List<WebsocketOutbound>> subscribersByDocumentId
	) {
		return (documentId, wsOutbound) -> subscribersByDocumentId.compute(documentId, (id, v) -> {
			var subscribers = v == null ? Collections.synchronizedList(new ArrayList<WebsocketOutbound>()) : v;
			subscribers.add(wsOutbound);
			return subscribers;
		});
	}

	@Provides
	@Named("port")
	public static int getPort() {
		return Integer.parseInt(System.getenv("PORT"));
	}

	private static int getIoThreads() {
		return Optional.ofNullable(System.getenv("IO_THREADS")).map(Integer::parseInt).orElse(IO_THREADS_DEFAULT);
	}

	private static int getWorkerThreads() {
		return Optional.ofNullable(System.getenv("WORKER_THREADS")).map(Integer::parseInt).orElse(WORKER_THREADS_DEFAULT);
	}

}
