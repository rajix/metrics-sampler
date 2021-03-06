package org.metricssampler.extensions.webmethods.parser;

import java.util.List;
import java.util.Map;

import org.metricssampler.extensions.webmethods.WebMethodsInputConfig;
import org.metricssampler.reader.MetricName;
import org.metricssampler.reader.MetricValue;

public class ServerStatsParser extends AbstractFileWithHeaderParser {
	public static final String ENTRY_RUNTIME_SERVER_STATS = "runtime/ServerStats.txt";

	public ServerStatsParser(final WebMethodsInputConfig config) {
		super(config, "ServerStats");
	}

	@Override
	protected void parseLine(final Map<MetricName, MetricValue> metrics, final List<String> lines, final int lineNum, final String line, final long timestamp) {
		if (line.length() > 0) {
			final int colIdx = line.indexOf(':');
			if (colIdx == -1) {
				lastSection = line.trim();
			} else {
				final String name = line.substring(0, colIdx);
				final String value = line.substring(colIdx + 2);
				final StringBuilder metricNameBuilder = new StringBuilder();
				if (name.startsWith(" ")) {
					if (lastSection != null) {
						metricNameBuilder.append(lastSection).append('.');
					}
				} else {
					lastSection = null;
				}
				metricNameBuilder.append(name);

				final MetricName metricName = createMetricName(metricNameBuilder);
				final String actualValue = parseValue(metricName.getName(), value);
				metrics.put(metricName, new MetricValue(timestamp, actualValue));
			}
		}
	}

	@Override
	public boolean canParseEntry(final String name) {
		return ENTRY_RUNTIME_SERVER_STATS.equals(name);
	}
}
