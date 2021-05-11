package it.unive.lisa;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.impl.Pentagons;
import it.unive.lisa.analysis.impl.StrictUpperBound;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import org.junit.Test;

public class SubTest {
    @Test
    public void testSub() throws ParsingException, AnalysisException {

        LiSAConfiguration configuration = new LiSAConfiguration();
        configuration.setDumpAnalysis(true);
        configuration.setWorkdir("test-outputs/sub-domain");
        configuration.setAbstractState(LiSAFactory.getDefaultFor(AbstractState.class,
                LiSAFactory.getDefaultFor(HeapDomain.class), new StrictUpperBound()));
        LiSA lisa = new LiSA(configuration);
        lisa.run(IMPFrontend.processFile("upper-bounds.imp"));
    }
}
