package org.metricssampler.daemon.commands;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.metricssampler.daemon.SamplerTask;

public class EnableSamplerCommand extends SamplerTaskCommand {
	private final int times;
	private final long seconds;

	protected EnableSamplerCommand(final BufferedReader reader, final BufferedWriter writer, final Map<String, SamplerTask> tasks, final String name, final int times, final long seconds) {
		super(reader, writer, tasks, name);
		this.times = times;
		this.seconds = seconds;
	}

	@Override
	public void execute() throws IOException {
		executeOnMatchingTasks(new SamplerTaskAction() {
			@Override
			public void execute(final SamplerTask task, final BufferedWriter writer) throws IOException {
				if (seconds != -1) {
					logger.info("Enabling sampler \"{}\" for {} seconds", task.getName(), seconds);
					task.enableForDuration(seconds);
				} else if (times == -1) {
					logger.info("Enabling sampler \"{}\"", task.getName(), times);
					task.enableForTimes(-1);
				} else {
					logger.info("Enabling sampler \"{}\" for {} samplings", task.getName(), times);
					task.enableForTimes(times);
				}
			}
		});
	}


}