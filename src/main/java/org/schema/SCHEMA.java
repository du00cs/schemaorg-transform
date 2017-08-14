package org.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

import com.sun.xml.internal.fastinfoset.sax.Properties;

public class SCHEMA {
	
	private static final String BASE_URI = "http://schema.org/";

	public static final Resource Class = ResourceFactory.createResource(BASE_URI + "Class");
	public static final Resource Thing = ResourceFactory.createResource(BASE_URI + "Thing");
	public static final Resource DataType = ResourceFactory.createResource(BASE_URI + "DataType");
	public static final Resource Boolean = ResourceFactory.createResource(BASE_URI + "Boolean");
	public static final Resource Text = ResourceFactory.createResource(BASE_URI + "Text");
	public static final Resource Number = ResourceFactory.createResource(BASE_URI + "Number");
	public static final Resource Date = ResourceFactory.createResource(BASE_URI + "Date");
	public static final Resource DateTime = ResourceFactory.createResource(BASE_URI + "DateTime");
	public static final Resource Time = ResourceFactory.createResource(BASE_URI + "Time");
	
	public static final Property domainIncludes = ResourceFactory.createProperty(BASE_URI + "domainIncludes");
	public static final Property rangeIncludes = ResourceFactory.createProperty(BASE_URI + "rangeIncludes");
	public static final Property inverseOf = ResourceFactory.createProperty(BASE_URI + "inverseOf");
	public static final Property name = ResourceFactory.createProperty(BASE_URI + "name");
	public static final Property description = ResourceFactory.createProperty(BASE_URI + "description");
	
	public static OntModel toRDFS(Model schemaorg) {
    	OntModel rdfs = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
    	
    	StmtIterator classes = schemaorg.listStatements(null, RDF.type, RDFS.Class);
    	while (classes.hasNext()) {
    		Resource next = classes.next().getSubject();
    		OntClass c = rdfs.createClass(next.getURI());
    		
    		StmtIterator statements = next.listProperties();
    		while (statements.hasNext()) {
    			Statement st = statements.next();
    			
    			if (st.getPredicate().equals(RDFS.subClassOf)) {
    				c.addSuperClass(st.getResource());
    			}
    		}
    		
    		StmtIterator enumeration = schemaorg.listStatements(null, RDF.type, next);
    		while (enumeration.hasNext()) {
    			rdfs.createIndividual(enumeration.next().getSubject().getURI(), c);
    		}
    	}
    	
    	StmtIterator datatypes = schemaorg.listStatements(null, RDF.type, DataType);
    	while (datatypes.hasNext()) {
    		Resource dt = datatypes.next().getSubject();
    		rdfs.createClass(dt.getURI()).setRDFType(RDFS.Datatype);
    	}
    	
    	StmtIterator properties = schemaorg.listStatements(null, RDF.type, RDF.Property);
    	while (properties.hasNext()) {
    		Resource next = properties.next().getSubject();
    		OntProperty p = rdfs.createOntProperty(next.getURI());
    		
    		List<Resource> domain = new ArrayList<Resource>();
    		List<Resource> range = new ArrayList<Resource>();
    		
    		StmtIterator statements = next.listProperties();
    		while (statements.hasNext()) {
    			Statement st = statements.next();
    			
    			if (st.getPredicate().equals(RDFS.subPropertyOf)) {
    				p.addSuperProperty((Property) st.getResource()); 
    			} else if (st.getPredicate().equals(domainIncludes)) {
    				domain.add(st.getResource());
    			} else if (st.getPredicate().equals(rangeIncludes)) {
    				range.add(st.getResource());
    			}
    		}
    		
    		OntClass domainUnion = rdfs.createClass(BASE_URI + getCanonicalName(domain));
    		p.setDomain(domainUnion);
    		for (Resource c : domain) {
    			rdfs.createClass(c.getURI()).addSuperClass(domainUnion);
    		}
    		
    		OntClass rangeUnion = rdfs.createClass(BASE_URI + getCanonicalName(range));
    		p.setRange(rangeUnion);
    		for (Resource c : range) {
    			rdfs.createClass(c.getURI()).addSuperClass(rangeUnion);
    		}
    	}
    	
    	// TODO keep RDFS annotation properties (label, comment)

    	return rdfs;
	}
    
    public static OntModel toOWL(Model schemaorg) {
    	OntModel owl = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
    	
    	OntModel rdfs = toRDFS(schemaorg);
    	
    	ExtendedIterator<OntClass> classes = rdfs.listClasses();
    	while (classes.hasNext()) {
    		OntClass c = classes.next();
    		
    		if (c.hasSubClass()) {
	    		RDFList sc = owl.createList(c.listSubClasses(true));
	    		owl.createUnionClass(c.getURI(), sc);
    		} else if (rdfs.contains(null, RDF.type, c)) {
        		RDFList i = owl.createList(c.listInstances(true));
        		owl.createEnumeratedClass(c.getURI(), i);
    		}
    	}
    	
    	ExtendedIterator<OntProperty> properties = rdfs.listOntProperties();
    	while (properties.hasNext()) {
    		OntProperty next = properties.next();
    		OntProperty p = next.getRange().hasRDFType(RDFS.Datatype) ?
    			owl.createDatatypeProperty(next.getURI()) :
    			owl.createObjectProperty(next.getURI());
    		
    		p.setDomain(next.getDomain());
    		p.setRange(toXSD(p.getRange()));
    		
    		if (next.hasProperty(inverseOf)) {
    			p.setInverseOf((Property) next.getPropertyResourceValue(inverseOf));
    		}
    		
    		if (next.hasProperty(RDFS.subPropertyOf)) {
    			p.setSuperProperty((Property) next.getPropertyResourceValue(RDFS.subPropertyOf));
    		}
    	}

        // schema:Thing has been mapped to owl:Thing
        // schema:name has been mapped to rdfs:label
        // schema:description has been mapped to rdfs:comment
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
    
    private static Resource toXSD(Resource schemaType) {
    	if (schemaType.equals(Boolean)) {
    		return XSD.xboolean;
    	} else if (schemaType.equals(Text)) {
    		return XSD.xstring;
    	} else if (schemaType.equals(Number)) {
    		return XSD.xdouble; // note: consequences on encoding
    	} else if (schemaType.equals(Date)) {
    		return XSD.date;
    	} else if (schemaType.equals(DateTime)) {
    		return XSD.dateTime;
    	} else if (schemaType.equals(Time)) {
    		return XSD.time;
    	} else {
        	return schemaType;
    	}
    }
    
    private static String getCanonicalName(List<Resource> resources) {
    	String name = "";
    	
    	Iterator<Resource> it = resources.iterator();
    	while (it.hasNext()) {
    		name += it.next().getLocalName() + (it.hasNext() ? "Or" : "");
    	}
    	
    	return name;
    }
	
	public static String getURI() {
		return BASE_URI;
	}

}
