package it.unive.lisa;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

import it.unive.lisa.checks.warnings.Warning;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.outputs.JsonReport;
import it.unive.lisa.program.Program;
import it.unive.lisa.util.file.FileManager;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is the central class of the LiSA library. While LiSA's functionalities
 * can be extended by providing additional implementations for each component,
 * code executing LiSA should rely solely on this class to engage the analysis,
 * provide inputs to it and retrieve its results.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class LiSA {

	private static final Logger log = LogManager.getLogger(LiSA.class);

	/**
	 * The collection of warnings that will be filled with the results of all
	 * the executed checks
	 */
	private final Collection<Warning> warnings;

	/**
	 * The {@link FileManager} instance that will be used during analyses
	 */
	private final FileManager fileManager;

	/**
	 * The {@link LiSAConfiguration} containing the settings of the analysis to
	 * run
	 */
	private final LiSAConfiguration conf;

	/**
	 * Builds a new LiSA instance.
	 * 
	 * @param conf the configuration of the analysis to run
	 */
	public LiSA(LiSAConfiguration conf) {
		// since the warnings collection will be filled AFTER the execution of
		// every concurrent bit has completed its execution, it is fine to use a
		// non thread-safe one
		this.warnings = new ArrayList<>();
		this.conf = conf;
		this.fileManager = new FileManager(conf.getWorkdir());
	}

	/**
	 * Runs LiSA, executing all the checks that have been added.
	 * 
	 * @param program the program to analyze
	 * 
	 * @throws AnalysisException if anything goes wrong during the analysis
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(Program program) throws AnalysisException {
		printConfig();

		CallGraph callGraph;
		try {
			callGraph = conf.getCallGraph() == null ? getDefaultFor(CallGraph.class) : conf.getCallGraph();
			if (conf.getCallGraph() == null)
				log.warn("No call graph set for this analysis, defaulting to " + callGraph.getClass().getSimpleName());
		} catch (AnalysisSetupException e) {
			throw new AnalysisExecutionException("Unable to create default call graph", e);
		}

		InterproceduralAnalysis interproc;
		try {
			interproc = conf.getInterproceduralAnalysis() == null ? getDefaultFor(InterproceduralAnalysis.class)
					: conf.getInterproceduralAnalysis();
			if (conf.getInterproceduralAnalysis() == null)
				log.warn("No interprocedural analysis set for this analysis, defaulting to "
						+ interproc.getClass().getSimpleName());
		} catch (AnalysisSetupException e) {
			throw new AnalysisExecutionException("Unable to create default interprocedural analysis", e);
		}

		LiSARunner runner = new LiSARunner(conf, interproc, callGraph, conf.getState());

		try {
			warnings.addAll(TimerLogger.execSupplier(log, "Analysis time", () -> runner.run(program, fileManager)));
		} catch (AnalysisExecutionException e) {
			throw new AnalysisException("LiSA has encountered an exception while executing the analysis", e);
		}

		printStats();

		if (conf.isJsonOutput()) {
			log.info("Dumping reported warnings to 'report.json'");
			JsonReport report = new JsonReport(warnings, fileManager.createdFiles());
			try (Writer writer = fileManager.mkOutputFile("report.json")) {
				report.dump(writer);
				log.info("Report file dumped to report.json");
			} catch (IOException e) {
				log.error("Unable to dump report file", e);
			}
		}
	}

	private void printConfig() {
		log.info(conf.toString());
	}

	private void printStats() {
		log.info("LiSA statistics:");
		log.info("  " + warnings.size() + " warnings generated");
	}

	/**
	 * Yields an unmodifiable view of the warnings that have been generated
	 * during the analysis. Invoking this method before invoking
	 * {@link #run(Program)} will return an empty collection.
	 * 
	 * @return a view of the generated warnings
	 */
	public Collection<Warning> getWarnings() {
		return Collections.unmodifiableCollection(warnings);
	}
}
