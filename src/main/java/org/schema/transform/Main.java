package org.schema.transform;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.cli.ParseException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;
import org.schema.SCHEMA;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class Main {
    public static class Option {
        @Parameter(names = "--namespace", description = "Use the given namespace for generated terms.") public String namespace = SCHEMA
            .getURI();

        @Parameter(names = "--vocabulary", description = "Transform the given cnschema.org vocabulary into an RDFS or OWL ontology.") public String vocabulary = "RDFS";

        @Parameter(names = "--input", required = true, description = "input file") public String input;

        @Parameter(names = "--output", description = "output file, default to console") public String output = null;

        @Parameter(names = "--input-syntax", description = "syntax in Jena: JSON/LD, RDF/XML, ...") public String inputSyntax = null;

        @Parameter(names = "--output-syntax", description = "output syntax, one of Jena defined, default turtle") public String outputSyntax = "Turtle";
    }

    public static void main(String[] args) throws ParseException, IOException {

        Option opt = new Option();
        JCommander.newBuilder().addObject(opt).build().parse(args);

        Model voc = FileManager.get().loadModel(opt.input, null, opt.inputSyntax);

        OutputStream out;
        if (opt.output == null)
            out = System.out;
        else
            out = Files.newOutputStream(Paths.get(opt.output));

        if (Objects.equals(opt.vocabulary, "OWL")) {
            Model owl = SCHEMA.toOWL(voc, opt.namespace);
            owl.write(out, "Turtle");
        } else if (Objects.equals(opt.vocabulary, "RDFS")) {
            Model rdfs = SCHEMA.toRDFS(voc, opt.namespace);
            rdfs.write(out, "Turtle");
        } else {
            System.err.println("vocabulary should be one of RDFS/OWL");
        }
        out.close();
    }

    public static void show(Model model) {
        System.out.println("show model");
        StmtIterator iter = model.listStatements();
        try {
            while (iter.hasNext()) {
                Statement stmt = iter.next();

                Resource s = stmt.getSubject();
                Resource p = stmt.getPredicate();
                RDFNode o = stmt.getObject();

                if (s.isURIResource()) {
                    System.out.print("URI[" + s.getURI() + "]");
                } else if (s.isAnon()) {
                    System.out.print("blank");
                }

                if (p.isURIResource())
                    System.out.print(" URI[" + p.getURI() + "] ");

                if (o.isURIResource()) {
                    System.out.print("URI[" + o.toString() + "]");
                } else if (o.isAnon()) {
                    System.out.print("blank");
                } else if (o.isLiteral()) {
                    System.out.print("[" + o.toString() + "]");
                }

                System.out.println();
            }
        } finally {
            if (iter != null)
                iter.close();
        }
    }
}
