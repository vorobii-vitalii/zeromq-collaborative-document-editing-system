package org.example.modules;

import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Module
public class MetricsModule {

	@Provides
	MeterRegistry meterRegistry() {
		var meterRegistry = new SimpleMeterRegistry();
		Metrics.addRegistry(meterRegistry);
		return meterRegistry;
	}

}
