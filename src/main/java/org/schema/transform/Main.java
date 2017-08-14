package org.schema.transform;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.schema.SCHEMA;

public class Main {
	
	public static void main(String[] args) throws ParseException {
		Options opts = new Options()
			.addOption("h", "help", false, "Display this help.")
			.addOption("o", "owl", false, "Transform the given schema.org vocabulary into a restrictive OWL ontology.")
			.addOption("r", "rdfs", false, "Transform the given schema.org vocabulary into an RDFS vocabulary.");
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(opts, args);
		
		String uri = cmd.getArgList().get(0);
		Model voc = RDFDataMgr.loadDataset(uri).getDefaultModel();
		
		if (cmd.hasOption('+')) {
			SCHEMA.toOWL(voc).write(System.out);
		} else if (cmd.hasOption('-')) {
			SCHEMA.toRDFS(voc).write(System.out);
		}
		
		HelpFormatter help = new HelpFormatter();
		help.printHelp("transform [OPTS] <dataset URI>", "", opts, "");
	}

}
