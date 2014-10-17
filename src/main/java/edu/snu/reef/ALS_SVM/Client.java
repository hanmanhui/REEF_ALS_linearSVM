package edu.snu.reef.ALS_SVM;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.hadoop.mapred.TextInputFormat;

import com.microsoft.reef.client.DriverConfiguration;
import com.microsoft.reef.client.DriverLauncher;
import com.microsoft.reef.client.LauncherStatus;
import com.microsoft.reef.driver.evaluator.EvaluatorRequest;
import com.microsoft.reef.io.data.loading.api.DataLoadingRequestBuilder;
import com.microsoft.reef.runtime.local.client.LocalRuntimeConfiguration;
import com.microsoft.reef.runtime.yarn.client.YarnClientConfiguration;
import com.microsoft.reef.util.EnvironmentUtils;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.Injector;
import com.microsoft.tang.JavaConfigurationBuilder;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;
import com.microsoft.tang.formats.CommandLine;

public final class Client {
	
	private static final int NUM_LOCAL_THREAD = 16;
	private static final int NUM_SPLITS = 6;
	private static final int NUM_COMPUTE_EVALUATORS = 2;
	
	private static final Logger LOG = Logger.getLogger(Client.class.getName());

	@NamedParameter(doc = "Whether or not to run on the local runtime", 
			short_name = "local", default_value = "true")
	public static final class Local implements Name<Boolean> {
	}
	
	@NamedParameter(doc = "Number of minutes before timeout", 
			short_name = "timeout", default_value = "2")
	public static final class TimeOut implements Name<Integer> {
	}
	
	@NamedParameter(short_name = "input")
	public static final class InputDir implements Name<String> {
	}
		
	public static void main(final String[] args)
			throws BindException, InjectionException, IOException {
		
		final Tang tang = Tang.Factory.getTang();
		
		final JavaConfigurationBuilder cb = tang.newConfigurationBuilder();
		
		new CommandLine(cb)
			.registerShortNameOfClass(Local.class)
			.registerShortNameOfClass(TimeOut.class)
			.registerShortNameOfClass(Client.InputDir.class)
			.processCommandLine(args);
		
		final Injector injector = tang.newInjector(cb.build());
		
		final Boolean isLocal = injector.getNamedInstance(Local.class);
		final Integer jobTimeout = injector.getNamedInstance(TimeOut.class) * 60 * 1000;
		final String inputDir = injector.getNamedInstance(Client.InputDir.class);
		
		final Configuration runtimeConfiguration;
		if(isLocal) {
			runtimeConfiguration = LocalRuntimeConfiguration.CONF
					.set(LocalRuntimeConfiguration.NUMBER_OF_THREADS, NUM_LOCAL_THREAD)
					.build();
		} else {
			runtimeConfiguration = YarnClientConfiguration.CONF.build();
		}
		
		final EvaluatorRequest computeRequest = EvaluatorRequest.newBuilder()
				.setNumber(NUM_COMPUTE_EVALUATORS)
				.setMemory(512)
				.build();
		
		final Configuration dataLoadingConfiguration = new DataLoadingRequestBuilder()
			.setMemoryMB(1024)
			.setInputFormatClass(TextInputFormat.class)
			.setInputPath(inputDir)
			.setNumberOfDesiredSplits(NUM_SPLITS)
			.setComputeRequest(computeRequest)
			.setDriverConfigurationModule(DriverConfiguration.CONF
					.set(DriverConfiguration.GLOBAL_LIBRARIES, EnvironmentUtils.getClassLocation(SMSDataLoader.class))
					.set(DriverConfiguration.ON_CONTEXT_ACTIVE, SMSDataLoader.ContextActiveHandler.class)
					.set(DriverConfiguration.ON_TASK_COMPLETED, SMSDataLoader.TaskCompletedHandler.class)
					.set(DriverConfiguration.DRIVER_IDENTIFIER, "SMSDataLoader"))
			.build();
		
		final LauncherStatus status = DriverLauncher.getLauncher(runtimeConfiguration).run(dataLoadingConfiguration, jobTimeout);
		
		LOG.log(Level.INFO, "REEF job completed: {0}", status);
	}
}
