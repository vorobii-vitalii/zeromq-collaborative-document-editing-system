package org.example.module;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;

class TestMetricsModule {

	MetricsModule metricsModule = new MetricsModule();

	@Test
	void meterRegistry() {
		MeterRegistry meterRegistry = metricsModule.meterRegistry();
		assertThat(Metrics.globalRegistry.getRegistries()).contains(meterRegistry);
	}
}
