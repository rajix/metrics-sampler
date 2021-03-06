package org.metricssampler.extensions.apachestatus;

import java.net.URL;
import java.util.Map;

import org.metricssampler.config.HttpInputConfig;

public class ApacheStatusInputConfig extends HttpInputConfig {

	protected ApacheStatusInputConfig(final String name, final Map<String, Object> variables, final URL url, final String username, final String password,
			final Map<String, String> headers, final boolean preemptiveAuthEnabled) {
		super(name, variables, url, username, password, headers, preemptiveAuthEnabled);
	}
}
