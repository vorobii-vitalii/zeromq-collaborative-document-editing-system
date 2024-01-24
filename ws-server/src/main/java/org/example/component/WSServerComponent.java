package org.example.component;

import javax.inject.Singleton;

import org.example.module.WebSocketServerModule;
import org.example.process.DocumentEventsReadProcess;
import org.example.process.ProtocolSwitchProcess;

import dagger.Component;
import reactor.netty.http.server.HttpServer;

@Component(modules = {
		WebSocketServerModule.class
})
@Singleton
public interface WSServerComponent {
	HttpServer webSocketServer();
	DocumentEventsReadProcess documentEventsReadProcess();
	ProtocolSwitchProcess protocolSwitchProcess();
}
