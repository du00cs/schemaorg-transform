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
import org.apache.jena.sparql.util.ModelUtils;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.schema.SCHEMA;

public class Main {
	
	public static void main(String[] args) throws ParseException {	     
		Options opts = new Options()
			.addOption("h", "help", false, "Display this help.")
			.addOption("n", "namespace", true, "Use the given namespace for generated terms.")
			.addOption("O", "owl", false, "Transform the given schema.org vocabulary into a restrictive OWL ontology.")
			.addOption("R", "rdfs", false, "Transform the given schema.org vocabulary into an RDFS vocabulary.");
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(opts, args);
		
		if (cmd.getArgList().size() > 0) {
			String arg = cmd.getArgList().get(0);
			Model voc = FileManager.get().loadModel(arg);
			
			String ns = cmd.hasOption('n') ? cmd.getOptionValue('n') : SCHEMA.getURI();
			
			if (cmd.hasOption('O')) {
				Model owl = SCHEMA.toOWL(voc, ns);
				owl.write(System.out, "Turtle");
				return;
			} else if (cmd.hasOption('R')) {
				Model rdfs = SCHEMA.toRDFS(voc, ns);
				rdfs.write(System.out, "Turtle");
				return;
			}
		}
		
		HelpFormatter help = new HelpFormatter();
		help.printHelp("transform [OPTS] <dataset URI>", "", opts, "");
	}

}
