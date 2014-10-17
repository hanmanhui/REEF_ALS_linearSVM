package edu.snu.reef.ALS_SVM;

import javax.inject.Inject;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;

import com.microsoft.reef.annotations.audience.TaskSide;
import com.microsoft.reef.io.data.loading.api.DataSet;
import com.microsoft.reef.io.network.util.Utils.Pair;
import com.microsoft.reef.task.Task;

@TaskSide
public class SMSDataLoadTask implements Task {
	private static final Logger LOG = Logger.getLogger(SMSDataLoadTask.class);
	
	private final DataSet<LongWritable, Text> dataSet;
	
	@Inject
	public SMSDataLoadTask(final DataSet<LongWritable, Text> dataSet) {
		this.dataSet = dataSet;
	}

	@Override
	public byte[] call(final byte[] memento) throws Exception {
		// TODO Auto-generated method stub
		for(final Pair<LongWritable, Text> keyValue : dataSet) {
			System.out.println("[BDCS] Key : " + keyValue.first + " Value : " + keyValue.second);
		}
		
		return null;
	}

}
