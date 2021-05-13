package it.unive.lisa.program.cfg;

import static org.junit.Assert.fail;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.impl.heap.MonolithicHeap;
import it.unive.lisa.analysis.impl.numeric.Sign;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.callgraph.impl.intraproc.IntraproceduralCallGraph;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.util.datastructures.graph.FixpointException;
import org.junit.Test;

public class FixpointTest {

	private IntraproceduralCallGraph mkCallGraph() {
		return new IntraproceduralCallGraph();
	}

	private AnalysisState<SimpleAbstractState<MonolithicHeap, ValueEnvironment<Sign>>, MonolithicHeap,
			ValueEnvironment<Sign>> mkState() {
		return new AnalysisState<>(new SimpleAbstractState<>(new MonolithicHeap(), new ValueEnvironment<>(new Sign())),
				new ExpressionSet<>());
	}

	@Test
	public void testEmptyCFG() {
		SourceCodeLocation unknownLocation = new SourceCodeLocation("fake", 0, 0);
		CFG cfg = new CFG(
				new CFGDescriptor(unknownLocation, new CompilationUnit(unknownLocation, "foo", false), false, "foo"));
		try {
			cfg.fixpoint(mkState(), mkCallGraph());
		} catch (FixpointException e) {
			System.err.println(e);
			fail("The fixpoint computation has thrown an exception");
		}
	}

	@Test
	public void testEmptyIMPMethod() throws ParsingException {
		Program p = IMPFrontend.processText("class empty { foo() { } }");
		CFG cfg = p.getAllCFGs().iterator().next();
		try {
			cfg.fixpoint(mkState(), mkCallGraph());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}
	}

	@Test
	public void testIMPMethodWithEmptyIfBranch() throws ParsingException {
		Program p = IMPFrontend.processText("class empty { foo() { if (true) { this.foo(); } else {} } }");
		CFG cfg = p.getAllCFGs().iterator().next();
		try {
			cfg.fixpoint(mkState(), mkCallGraph());
		} catch (FixpointException e) {
			e.printStackTrace(System.err);
			fail("The fixpoint computation has thrown an exception");
		}
	}
}
