package org.example.components;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.example.module.DocumentServerModule;
import org.example.module.MetricsModule;
import org.example.module.MongoModule;
import org.example.server.ZeroMQRequestReplyServer;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import dagger.Component;

@Singleton
@Component(modules = {MongoModule.class, MetricsModule.class, DocumentServerModule.class})
public interface DocumentServerComponent {
	List<ZeroMQRequestReplyServer> createDocumentRequestReplyWorkers();

	@Named("routerSocket")
	ZMQ.Socket routerSocket();

	@Named("dealerSocket")
	ZMQ.Socket dealerSocket();

	ZContext context();
}
