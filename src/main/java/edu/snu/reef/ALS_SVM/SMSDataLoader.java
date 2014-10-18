package edu.snu.reef.ALS_SVM;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.microsoft.reef.annotations.audience.DriverSide;
import com.microsoft.reef.driver.context.ActiveContext;
import com.microsoft.reef.driver.context.ContextConfiguration;
import com.microsoft.reef.driver.task.CompletedTask;
import com.microsoft.reef.driver.task.TaskConfiguration;
import com.microsoft.reef.io.data.loading.api.DataLoadingService;
import com.microsoft.reef.poison.PoisonedConfiguration;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.Tang;
import com.microsoft.tang.annotations.Unit;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.wake.EventHandler;

@DriverSide
@Unit
public class SMSDataLoader {
	private static final Logger LOG = Logger.getLogger(SMSDataLoader.class.getName());
	
	private final AtomicInteger ctrlCtxIds = new AtomicInteger();
	private final AtomicInteger completedDataTasks = new AtomicInteger();
	
	private final DataLoadingService dataLoadingService;
	
	@Inject
	public SMSDataLoader(final DataLoadingService dataLoadingService) {
		this.dataLoadingService = dataLoadingService;
		this.completedDataTasks.set(dataLoadingService.getNumberOfPartitions());
	}
	
	public class ContextActiveHandler implements EventHandler<ActiveContext> {

		@Override
		public void onNext(final ActiveContext activeContext) {
			// TODO Auto-generated method stub
			final String contextId = activeContext.getId();
			
			if(dataLoadingService.isDataLoadedContext(activeContext)) {
				final String lcContextId = "DataLoadCtxt-" + ctrlCtxIds.getAndIncrement();
				final Configuration poisonedConfiguration = PoisonedConfiguration.CONTEXT_CONF
						.set(PoisonedConfiguration.CRASH_PROBABILITY, "0.4")
						.set(PoisonedConfiguration.CRASH_TIMEOUT, "1")
						.build();
				
				activeContext.submitContext(Tang.Factory.getTang()
						.newConfigurationBuilder(poisonedConfiguration, 
								ContextConfiguration.CONF.set(ContextConfiguration.IDENTIFIER, lcContextId).build())
							.build());
			} else if(activeContext.getId().startsWith("DataLoadCtxt")){
				final String taskId = "DataLoadTask-" + ctrlCtxIds.getAndIncrement();
				
				try {
					activeContext.submitTask(TaskConfiguration.CONF
							.set(TaskConfiguration.IDENTIFIER, taskId)
							.set(TaskConfiguration.TASK, SMSDataLoadTask.class)
							.build());
				} catch(final BindException ex) {
					throw new RuntimeException("Configuration error in " + contextId, ex);
				}
			}
		}
		
	}
	
	public class TaskCompletedHandler implements EventHandler<CompletedTask> {

		@Override
		public void onNext(final CompletedTask completedTask) {
			// TODO Auto-generated method stub
			final String taskId = completedTask.getId();
			
			final byte[] retBytes = completedTask.get();
			try {
				List<SMSVector> lv = (List<SMSVector>) ObjectToByteArray.deserialize(retBytes);
				System.out.println("[BDCS] Completed vectors : " + lv.size());
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(completedDataTasks.decrementAndGet() <= 0) {
				System.out.println("[BDCS] Finished");
			}
			
			completedTask.getActiveContext().close();
		}
		
	}
}
