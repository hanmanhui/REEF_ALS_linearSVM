package edu.snu.reef.ALS_SVM;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.microsoft.reef.driver.context.ContextConfiguration;
import com.microsoft.reef.driver.evaluator.AllocatedEvaluator;
import com.microsoft.reef.driver.evaluator.EvaluatorRequest;
import com.microsoft.reef.driver.evaluator.EvaluatorRequestor;
import com.microsoft.reef.driver.task.TaskConfiguration;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.annotations.Unit;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.wake.EventHandler;
import com.microsoft.wake.time.event.StartTime;

@Unit
public final class ReefDriver {
	
	private static final Logger LOG = Logger.getLogger(ReefDriver.class.getName());
	
	private EvaluatorRequestor requstor = null;
	
	@Inject
	public ReefDriver(final EvaluatorRequestor requestor) {
		LOG.log(Level.FINE, "Instantiated 'ReefDriver'");
		
		this.requstor = requestor;
	}

	public final class StartHandler implements EventHandler<StartTime> {

		@Override
		public void onNext(final StartTime startTime) {
			// TODO Auto-generated method stub
			LOG.log(Level.INFO, "Requested Evaluator");
			
			ReefDriver.this.requstor.submit(
					EvaluatorRequest.newBuilder()
					.setMemory(128)
					.setNumber(3)
					.build());
		}
		
	}
	
	public final class EvaluatorAllocatedHandler implements EventHandler<AllocatedEvaluator> {

		@Override
		public void onNext(final AllocatedEvaluator allocatedEvaluator) {
			// TODO Auto-generated method stub
			LOG.log(Level.INFO, "Submitting Reef task to AllocatedEvaluator: {0}", allocatedEvaluator);
			
			try {
				final Configuration contextConfiguration= ContextConfiguration.CONF
						.set(ContextConfiguration.IDENTIFIER, "ReefContext")
						.build();
				
				final Configuration taskConfiguration = TaskConfiguration.CONF
						.set(TaskConfiguration.IDENTIFIER, "ReefTask")
						.set(TaskConfiguration.TASK, ReefTask.class)
						.build();
				
				allocatedEvaluator.submitContextAndTask(contextConfiguration, taskConfiguration);
			} catch (final BindException ex) {
				throw new RuntimeException("Unable to setup Task or Context configuration.", ex);
			}
		}
	}
}
