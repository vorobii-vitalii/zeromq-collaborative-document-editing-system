package org.example.module;

import javax.inject.Named;
import javax.inject.Singleton;

import org.example.constants.ServerConstants;
import org.example.process.ProtocolSwitchProcess;
import org.example.server.ServiceCaller;
import org.example.server.impl.DocumentServiceCaller;
import org.example.server.impl.ZMQServiceCaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;

import dagger.Module;
import dagger.Provides;

@Module(includes = ZeroMQModule.class)
public class DocumentsServerModule {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsServerModule.class);

	@Provides
	ProtocolSwitchProcess protocolSwitchProcess(
			@Named("documentServerURL") String documentServerURL,
			ZContext context
	) {
		var routerSocket = context.createSocket(SocketType.ROUTER);
		routerSocket.bind(ServerConstants.REQUESTS_INPROC_SOCKET);
		var dealerSocket = context.createSocket(SocketType.DEALER);
		dealerSocket.connect(documentServerURL);
		LOGGER.info("Creating protocol switcher from {} to {}", ServerConstants.REQUESTS_INPROC_SOCKET, documentServerURL);
		return new ProtocolSwitchProcess(routerSocket, dealerSocket);
	}

	@Singleton
	@Provides
	ServiceCaller<byte[]> documentServerCaller(ZContext context) {
		return new ZMQServiceCaller<>(() -> {
			var socket = context.createSocket(SocketType.REQ);
			socket.connect(ServerConstants.REQUESTS_INPROC_SOCKET);
			return socket;
		}, new DocumentServiceCaller());
	}

	@Provides
	@Named("documentServerURL")
	public String getDocumentServerURL() {
		return System.getenv("DOCUMENT_SERVER_URL");
	}

	@Provides
	@Named("minPoolSize")
	public int getMinPoolSize() {
		return Integer.parseInt(System.getenv("MIN_POOL_SIZE"));
	}

	@Provides
	@Named("maxPoolSize")
	public int getMaxPoolSize() {
		return Integer.parseInt(System.getenv("MAX_POOL_SIZE"));
	}

}
