package org.metricssampler.extensions.base.sampler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.metricssampler.config.Placeholder;
import org.metricssampler.reader.MetricReadException;
import org.metricssampler.reader.MetricValue;
import org.metricssampler.reader.MetricsReader;
import org.metricssampler.reader.OpenMetricsReaderException;
import org.metricssampler.sampler.Sampler;
import org.metricssampler.selector.MetricsSelector;
import org.metricssampler.selector.PlaceholderReplacer;
import org.metricssampler.writer.MetricWriteException;
import org.metricssampler.writer.MetricsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class DefaultSampler implements Sampler {
	private final Logger logger;
	private final Logger timingsLogger;

	private final DefaultSamplerConfig config;
	private final MetricsReader reader;
	private final List<MetricsWriter> writers = new LinkedList<MetricsWriter>();
	private final List<MetricsSelector> selectors = new LinkedList<MetricsSelector>();

	private final List<Placeholder> placeholders;
	
	public DefaultSampler(final DefaultSamplerConfig config, final MetricsReader reader, final List<Placeholder> placeholders) {
		this.config = config;
		this.reader = reader;
		this.placeholders = placeholders;
		logger = LoggerFactory.getLogger("sampler."+this.config.getName());
		timingsLogger = LoggerFactory.getLogger("timings.sampler");
	}

	public DefaultSampler addWriter(final MetricsWriter writer) {
		writers.add(writer);
		return this;
	}

	public DefaultSampler addSelector(final MetricsSelector selector) {
		selectors.add(selector);
		final Map<String, Object> transformerPlaceholders = new HashMap<String, Object>();
		final Map<String, Object> readerPlaceholders = reader.getPlaceholders();
		transformerPlaceholders.putAll(readerPlaceholders);
		for (final Placeholder placeholder : placeholders) {
			transformerPlaceholders.put(placeholder.getKey(), PlaceholderReplacer.replace((String)placeholder.getValue(), transformerPlaceholders));
		}
		selector.setPlaceholders(transformerPlaceholders);
		return this;
	}

	protected void openWriters() {
		for (final MetricsWriter writer : writers) {
			writer.open();
		}
	}

	protected void closeWriters() {
		for (final MetricsWriter writer : writers) {
			writer.close();
		}
	}

	@Override
	public void sample() {
		MDC.put("sampler", config.getName());
		logger.debug("Sampling");
		try {
			final long readStart = System.currentTimeMillis();
			final Map<String, MetricValue> metrics = readMetrics();
			final long readEnd = System.currentTimeMillis();
			timingsLogger.debug("Sampled {} metrics in {} ms", metrics.size(), readEnd-readStart);
			writeMetrics(metrics);
			timingsLogger.debug("Metrics sent to writers in {} ms", System.currentTimeMillis()-readEnd);
		} catch (final OpenMetricsReaderException e) {
			if (!config.isQuiet()) {
				logger.warn("Failed to open reader", e);
			} else {
				logger.debug("Failed to open reader", e);
			}
		} catch (final MetricReadException e) {
			logger.warn("Failed to read metrics", e);
		} catch (final MetricWriteException e) {
			logger.warn("Failed to write metrics", e);
		}
		MDC.remove("sampler");
	}

	private void writeMetrics(final Map<String, MetricValue> metrics) {
		openWriters();

		for (final MetricsWriter writer : writers) {
			try {
				logger.debug("Writing metrics to " + writer);
				writer.write(metrics);
			} catch(final MetricWriteException e) {
				logger.warn("Failed to write metrics to "+writer);
			}
		}

		closeWriters();
	}

	private Map<String, MetricValue> readMetrics() {
		logger.debug("Opening reader {}", reader);
		reader.open();
		
		logger.debug("Reading metrics from {}", reader);
		final Map<String, MetricValue> result = new HashMap<String, MetricValue>();
		for (final MetricsSelector selector : selectors) {
			logger.debug("Reading metrics from {} via {}", reader, selector);
			final Map<String, MetricValue> metrics = selector.readMetrics(reader);
			logger.debug("Selector " + selector + " returned " + metrics.size() + " metrics for " + reader);
			result.putAll(metrics);
		}

		reader.close();

		return result;
	}

	@Override
	public boolean check() {
		boolean result = true;
		reader.open();

		for (final MetricsSelector transformer : selectors) {
			final int count = transformer.getMetricCount(this.reader);
			if (count == 0) {
				System.out.println(transformer + " has no metrics");
				result = false;
			} else {
				System.out.println(transformer + " matches " + count + " metrics");
			}
		}

		reader.close();
		return result;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"["+reader+"->"+writers+ "]";
	}
}
