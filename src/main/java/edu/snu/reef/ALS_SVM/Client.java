package edu.snu.reef.ALS_SVM;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.microsoft.reef.client.DriverConfiguration;
import com.microsoft.reef.client.DriverLauncher;
import com.microsoft.reef.client.LauncherStatus;
import com.microsoft.reef.runtime.local.client.LocalRuntimeConfiguration;
import com.microsoft.reef.util.EnvironmentUtils;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;

public final class Client {
	
	private static final Logger LOG = Logger.getLogger(Client.class.getName());
	
	private static final int JOB_TIMEOUT = 10000; // 10sec
	
	public static Configuration getDriverConfiguration() {
		final Configuration driverConfiguration = DriverConfiguration.CONF
				.set(DriverConfiguration.DRIVER_IDENTIFIER, "ALS_SVM")
				.set(DriverConfiguration.GLOBAL_LIBRARIES, EnvironmentUtils.getClassLocation(ReefDriver.class))
				.set(DriverConfiguration.ON_DRIVER_STARTED, ReefDriver.StartHandler.class)
				.set(DriverConfiguration.ON_EVALUATOR_ALLOCATED, ReefDriver.EvaluatorAllocatedHandler.class)
				.build();
		
		return driverConfiguration;
	}
	
	public static LauncherStatus run(final Configuration runtimeConf, final int timeout) 
			throws BindException, InjectionException {
		final Configuration driverConf = getDriverConfiguration();
		
		return DriverLauncher.getLauncher(runtimeConf).run(driverConf, timeout);
	}
	
	public static void main(final String[] args) throws BindException, InjectionException {
		final Configuration runtimeConfiguration = LocalRuntimeConfiguration.CONF
				.set(LocalRuntimeConfiguration.NUMBER_OF_THREADS, 3)
				.build();
		
		final LauncherStatus status = run(runtimeConfiguration, JOB_TIMEOUT);
		LOG.log(Level.INFO, "REEF job completed: {0}", status);
	}
}
