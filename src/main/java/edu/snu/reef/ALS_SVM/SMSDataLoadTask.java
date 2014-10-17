package edu.snu.reef.ALS_SVM;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.log4j.Logger;

import com.microsoft.reef.annotations.audience.TaskSide;
import com.microsoft.reef.io.data.loading.api.DataSet;
import com.microsoft.reef.io.network.util.Utils.Pair;
import com.microsoft.reef.task.Task;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;

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
			PTBTokenizer tokenizer = new PTBTokenizer(new StringReader(keyValue.second.toString()), new CoreLabelTokenFactory(), "");
			
			Map<String, Double> words = new HashMap<String, Double>();
			
			for(CoreLabel label; tokenizer.hasNext(); ) {
				label = (CoreLabel) tokenizer.next();
				if(label.word().length() < 2) {
					continue;
				} else {
					Double t = words.get(label.word());
					words.put(label.word(), 1 + (t == null ? 0 : t));
				}
			}
			// TF Calculation (Scaling)
			List<Double> values = new ArrayList<Double>(words.values());
			Collections.sort(values);
			Double maxCount = values.get(values.size()-1);
			
			for(String word : words.keySet()) {
				words.put(word, 0.5 * (1 + words.get(word)/maxCount));
				System.out.println("[BDCS] word : " + word + " value : " + words.get(word));
			}
		}
		
		return null;
	}

}
