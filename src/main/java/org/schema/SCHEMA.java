package org.schema;

import java.util.Set;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;

public class SCHEMA {
	
	private static final String BASE_URI = "http://schema.org/";

	public static final Resource Class = ResourceFactory.createResource(BASE_URI + "Class");
	public static final Resource Thing = ResourceFactory.createResource(BASE_URI + "Thing");
	
	public static final Property domainIncludes = ResourceFactory.createProperty(BASE_URI + "domainIncludes");
	public static final Property rangeIncludes = ResourceFactory.createProperty(BASE_URI + "rangeIncludes");
	public static final Property name = ResourceFactory.createProperty(BASE_URI + "name");
	public static final Property description = ResourceFactory.createProperty(BASE_URI + "description");
	
	public static OntModel toRDFS(OntModel m) {
		// TODO create new model instead
		OntModel inserted = ModelFactory.createOntologyModel();
		OntModel deleted = ModelFactory.createOntologyModel();

		// TODO schema.org's vocabulary instead
		ExtendedIterator<UnionClass> it = m.listUnionClasses();
		while (it.hasNext()) {
			UnionClass union = it.next();

			String name = "";
			ExtendedIterator<? extends OntClass> itOp = union.listOperands();
			while (itOp.hasNext()) {
				OntClass c = itOp.next();
				name += c.getLocalName();
				if (itOp.hasNext()) {
					name += "Or";
				}
			}
			OntClass merged = inserted.createClass(BASE_URI + name);
			
			itOp = union.listOperands();
			while (itOp.hasNext()) {
				OntClass c = itOp.next();
				merged.addSubClass(c);
			}
			
			ResIterator itDomain = m.listSubjectsWithProperty(RDFS.domain, union);
			while (itDomain.hasNext()) {
				Resource prop = itDomain.next();
				deleted.add(prop, RDFS.domain, union);
				inserted.add(prop, RDFS.domain, merged);
			}
			
			ResIterator itRange = m.listSubjectsWithProperty(RDFS.range, union);
			while (itRange.hasNext()) {
				Resource prop = itRange.next();
				deleted.add(prop, RDFS.range, union);
				inserted.add(prop, RDFS.range, merged);
			}
			
			deleted.add(union.listProperties());
			// TODO delete remaining blank nodes
		}
		
		m.add(inserted);
		m.remove(deleted);
		
		return m;
	}
    
    public static OntModel toOWL(OntModel schemaorg) {
    	OntModel owl = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);

    	ExtendedIterator<Individual> it = schemaorg.listIndividuals(Class);
    	while (it.hasNext()) {
    		Individual next = it.next();
            // schema:Thing has been mapped to owl:Thing
    		Resource c = next.equals(Thing) ? Thing : next;

            // schema:name has been mapped to rdfs:label
    		if (next.hasProperty(name)) {
    			owl.add(c, RDFS.label, next.getPropertyValue(name));
    		}

            // schema:description has been mapped to rdfs:comment
    		if (next.hasProperty(description)) {
    			owl.add(c, RDFS.comment, next.getPropertyValue(description));
    		}
    	}
    	
    	// TODO
    	
        // schema:url has been deleted - the URI of the subject should be used instead
        // XSD datatypes are used instead of schema datatypes
        // The types owl:Class, owl:ObjectProperty and owl:DatatypeProperty are used throughout
        // Legacy properties (e.g. in plural form) have been deleted
        // schema:Class, schema:Property and schema:additionalType have been deleted
        // rdfs:labels have been un-camelcased
        // Properties that had multiple schema:domainIncludes statements use a single owl:unionOf domain.
        // Properties that had multiple schema:rangeIncludes statements use a single owl:unionOf range.
        // Properties that either allowed numbers or strings have been mapped to xsd:float. Properties that either allowed xsd:string or URL have been mapped to xsd:anyURI.
    	
        return owl;
    }
	
	public static String getURI() {
		return BASE_URI;
	}

}
