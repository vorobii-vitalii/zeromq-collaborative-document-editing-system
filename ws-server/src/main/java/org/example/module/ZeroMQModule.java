package org.example.module;

import javax.inject.Singleton;

import org.zeromq.ZContext;

import dagger.Module;
import dagger.Provides;

@Module
public class ZeroMQModule {

	@Singleton
	@Provides
	ZContext context() {
		var context = new ZContext();
		Runtime.getRuntime().addShutdownHook(new Thread(context::close));
		return context;
	}

}
