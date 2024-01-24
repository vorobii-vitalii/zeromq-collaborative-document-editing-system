package org.example.module;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TestZeroMQModule {

	ZeroMQModule zeroMQModule = new ZeroMQModule();

	@Test
	void context() {
		var context = zeroMQModule.context();
		assertThat(context).isNotNull();
	}

}
