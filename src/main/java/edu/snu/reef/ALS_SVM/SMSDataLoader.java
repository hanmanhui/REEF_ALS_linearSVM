package edu.snu.reef.ALS_SVM;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

@DriverSide
@Unit
public class SMSDataLoader {
	private static final Logger LOG = Logger.getLogger(SMSDataLoader.class.getName());
	
	private final AtomicInteger ctrlCtxIds = new AtomicInteger();
	private final AtomicInteger completedDataTasks = new AtomicInteger();
	
	private final DataLoadingService dataLoadingService;
	
	private final Set<String> keywords = new HashSet<String>();
	private final List<SMSVector> listVector = new ArrayList<SMSVector>();
	
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
				for(SMSVector v : lv) {
					keywords.addAll(v.getKeys());
				}
				listVector.addAll(lv);
				System.out.println("[BDCS] Completed vectors : " + lv.size());
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(completedDataTasks.decrementAndGet() <= 0) {
				Problem problem = new Problem();
				problem.l = listVector.size();
				problem.n = keywords.size();
				
				List<String> keywordList = new ArrayList<String>(keywords);
				List<FeatureNode[]> featureNodes = new ArrayList<FeatureNode[]>();
				List<Integer> categories = new ArrayList<Integer>();
				
				List<FeatureNode[]> featureNodesTest = new ArrayList<FeatureNode[]>();
				List<Integer> categoriesTest = new ArrayList<Integer>();
				
				Random random = new Random();
				
				for(SMSVector v : listVector) {
					FeatureNode[] t = new FeatureNode[keywordList.size()];
					int i = 1;
					
					for(String key : keywordList) {
						Double val = v.getValue(key);
						t[i-1] = new FeatureNode(i, val == null ? 0 : val);
						i++;
					}
					

					if(random.nextDouble() < 0.9) {
						featureNodes.add(t);
						categories.add(v.getSpam());
					} else {
						featureNodesTest.add(t);
						categoriesTest.add(v.getSpam());						
					}
				}
				
				problem.x = (Feature[][]) featureNodes.toArray();
				
				for(int i = 0; i < categories.size(); i++) {
					problem.y[i] = categories.get(i);
				}
				
				SolverType solver = SolverType.L2R_LR;
				double C = 1.0;
				double eps = 0.01;
				
				Parameter parameter = new Parameter(solver, C, eps);
				Model model = Linear.train(problem, parameter);
				
				int matchCount = 0;
				for(int i = 0; i < featureNodesTest.size(); i++) {
					double prediction = Linear.predict(model, featureNodesTest.get(i));
					if(prediction == categoriesTest.get(i)) {
						matchCount++;
					}
				}
				System.out.println("[BDCS] Total of " + featureNodes.size() + " used as training set.");
				System.out.println("[BDCS] Hit Rate : " + matchCount/featureNodesTest.size());
				System.out.println("[BDCS] Finished");
			}
			
			completedTask.getActiveContext().close();
		}
		
	}
}
