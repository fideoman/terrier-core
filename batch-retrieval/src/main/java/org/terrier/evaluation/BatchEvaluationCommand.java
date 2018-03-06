package org.terrier.evaluation;

import java.io.File;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.terrier.applications.CLITool.CLIParsedCLITool;
import org.terrier.utility.ApplicationSetup;

import com.google.common.collect.Sets;

public class BatchEvaluationCommand extends CLIParsedCLITool {

	@Override
	public String helpsummary() {
		return "evaluate all run result files in the results directory";
	}

	@Override
	public String commandname() {
		return "batchevaluate";
	}

	@Override
	public Set<String> commandaliases() {
		return Sets.newHashSet("be");
	}

	@Override
	public int run(String[] args) throws Exception {
		Evaluation te = null;
		String evaluationFilename = null;
		boolean use_jtrec_eval = true;
		boolean evaluation_per_query = false;
		
		CommandLineParser parser = new DefaultParser();
		CommandLine line = null;
		try{
			line = parser.parse(getOptions(), args);
		} catch (ParseException pe) {
			System.err.println(pe);
			return 1;
		}
		
		String qrels = ApplicationSetup.getProperty("trec.qrels", null);
		if (line.hasOption('q'))
			qrels = line.getOptionValue('q');
		if (qrels == null)
		{
			System.err.println("No qrels specified in property trec.qrels or on command line (-q)");
			return 1;
		}
		if (line.hasOption('j'))
			use_jtrec_eval = false;
		if (use_jtrec_eval && TrecEvalEvaluation.isPlatformSupported()) {
			te = new TrecEvalEvaluation(qrels);
		}else{
			if (use_jtrec_eval)
				System.err.println("Sorry, your platform is not supported by jtrec_eval; resorting to older Terrier evaluation");
			te = new AdhocEvaluation(qrels);
			//else if (evaluation_type.equals("named"))
			//te = new NamedPageEvaluation();
		}
		String[] nomefile = null;
		if(line.getArgs().length > 0)
		{
			nomefile = line.getArgs();
		}
		
		if (nomefile == null) {
			/* list all the result files and then evaluate them */
			File fresdirectory = new File(ApplicationSetup.TREC_RESULTS);
			nomefile = fresdirectory.list();
			if (nomefile == null)
				nomefile = new String[0];
		}
		else
		{
			nomefile = new String[]{evaluationFilename};
		}
		for (int i = 0; i < nomefile.length; i++) {
			if (nomefile[i].endsWith(".res")) {
				String resultFilename = ApplicationSetup.TREC_RESULTS+ "/" + nomefile[i];
				if (nomefile[i].indexOf("/") >= 0)
					resultFilename = nomefile[i];
				String evaluationResultFilename =
					resultFilename.substring(
						0,
						resultFilename.lastIndexOf('.'))
						+ ".eval";
				te.evaluate(resultFilename);
				if (evaluation_per_query)
					te.writeEvaluationResultOfEachQuery(evaluationResultFilename);
				else
					te.writeEvaluationResult(evaluationResultFilename);
			}
		}
		return 0;
	}

	@Override
	protected Options getOptions() {
		Options options = new Options();
		options.addOption(Option.builder("j")
				.argName("no_jtreceval")
				.longOpt("jtreceval")
				.desc("disabled use of jtreceval.")
				.build());
		options.addOption(Option.builder("p")
				.argName("perquery")
				.longOpt("perquery")
				.desc("report results on a per-query basis")
				.build());
		options.addOption(Option.builder("q")
				.argName("qrels")
				.longOpt("qrels")
				.desc("specify location of qrels file.")
				.build());
		return options;
	}

}