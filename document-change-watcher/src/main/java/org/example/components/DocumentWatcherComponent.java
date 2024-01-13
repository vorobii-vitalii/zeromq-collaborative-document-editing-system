package org.example.components;

import org.bson.Document;
import org.example.modules.MetricsModule;
import org.example.modules.MongoModule;
import org.example.modules.PublishingServerModule;
import org.example.server.ZeroMQPublishingEventsServer;

import com.mongodb.client.model.changestream.ChangeStreamDocument;

import dagger.Component;

@Component(modules = {MetricsModule.class, MongoModule.class, PublishingServerModule.class})
public interface DocumentWatcherComponent {
	ZeroMQPublishingEventsServer<ChangeStreamDocument<Document>> createServer();
}
