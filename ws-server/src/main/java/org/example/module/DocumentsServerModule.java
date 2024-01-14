package org.example.module;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.example.server.impl.DocumentServiceCaller;
import org.example.server.impl.PooledReactiveZMQServiceCaller;
import org.example.server.ServiceCaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import dagger.Module;
import dagger.Provides;

@Module(includes = ZeroMQModule.class)
public class DocumentsServerModule {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentsServerModule.class);

	@Singleton
	@Provides
	GenericObjectPool<ZMQ.Socket> documentServerConnectionPool(
			ZContext context,
			@Named("documentServerURL") String documentServerURL,
			@Named("minPoolSize") int minPoolSize,
			@Named("maxPoolSize") int maxPoolSize
	) {
		var pooledObjectFactory = new BasePooledObjectFactory<ZMQ.Socket>() {
			@Override
			public ZMQ.Socket create() {
				var socket = context.createSocket(SocketType.REQ);
				LOGGER.info("Establishing connection to document server, url = {}", documentServerURL);
				socket.connect(documentServerURL);
				return socket;
			}

			@Override
			public PooledObject<ZMQ.Socket> wrap(ZMQ.Socket obj) {
				return new DefaultPooledObject<>(obj);
			}
		};

		var genericObjectPoolConfig = new GenericObjectPoolConfig<ZMQ.Socket>();
		genericObjectPoolConfig.setMinIdle(minPoolSize);
		genericObjectPoolConfig.setMaxTotal(maxPoolSize);

		var socketPool = new GenericObjectPool<>(pooledObjectFactory, genericObjectPoolConfig);
		Runtime.getRuntime().addShutdownHook(new Thread(socketPool::close));
		return socketPool;
	}

	@Singleton
	@Provides
	ServiceCaller<byte[]> documentServerCaller(GenericObjectPool<ZMQ.Socket> documentServerConnectionPool) {
		return new PooledReactiveZMQServiceCaller<>(documentServerConnectionPool, new DocumentServiceCaller());
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
