package it.unive.lisa.cron.dataflow;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.impl.dataflow.ConstantPropagation;
import org.junit.Test;

public class ConstantPropagationDFTest extends AnalysisTestExecutor {

	@Test
	public void testConstantPropagation() throws AnalysisSetupException {
		LiSAConfiguration conf = new LiSAConfiguration().setDumpAnalysis(true).setAbstractState(
				getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new ConstantPropagation()));
		perform("constant-propagation-df", "constant-propagation.imp", conf);
	}
}
