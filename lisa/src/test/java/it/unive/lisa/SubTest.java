package it.unive.lisa;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.impl.Pentagons;
import it.unive.lisa.analysis.impl.StrictUpperBound;
import it.unive.lisa.analysis.impl.numeric.Interval;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import org.junit.Test;

/*
    Coded by Eleonora Garbin 869831, Zonelli Mattia 870038.
*/

public class SubTest {
    @Test
    public void testSub() throws ParsingException, AnalysisException {

        LiSAConfiguration configuration = new LiSAConfiguration();
        configuration.setDumpAnalysis(true);
        configuration.setWorkdir("test-outputs/sub-domain");
        configuration.setAbstractState(LiSAFactory.getDefaultFor(AbstractState.class,
                LiSAFactory.getDefaultFor(HeapDomain.class), new Pentagons()));
        LiSA lisa = new LiSA(configuration);
        lisa.run(IMPFrontend.processFile("upper-bounds.imp"));
    }
}
