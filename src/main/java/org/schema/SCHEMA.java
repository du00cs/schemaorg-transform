package org.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.schema.transform.Main;

public class SCHEMA {
	
	private static final String BASE_URI = "http://cnschema.org/";

	public static final Resource Class = ResourceFactory.createResource(BASE_URI + "Class");
	public static final Resource Thing = ResourceFactory.createResource(BASE_URI + "Thing");
	public static final Resource DataType = ResourceFactory.createResource(BASE_URI + "DataType");
	public static final Resource Boolean = ResourceFactory.createResource(BASE_URI + "Boolean");
	public static final Resource True = ResourceFactory.createResource(BASE_URI + "True");
	public static final Resource False = ResourceFactory.createResource(BASE_URI + "False");
	public static final Resource Text = ResourceFactory.createResource(BASE_URI + "Text");
	public static final Resource URL = ResourceFactory.createResource(BASE_URI + "URL");
	public static final Resource Number = ResourceFactory.createResource(BASE_URI + "Number");
	public static final Resource Float = ResourceFactory.createResource(BASE_URI + "Float");
	public static final Resource Integer = ResourceFactory.createResource(BASE_URI + "Integer");
	public static final Resource Date = ResourceFactory.createResource(BASE_URI + "Date");
	public static final Resource DateTime = ResourceFactory.createResource(BASE_URI + "DateTime");
	public static final Resource Time = ResourceFactory.createResource(BASE_URI + "Time");
	
	public static final Property domainIncludes = ResourceFactory.createProperty(BASE_URI + "domainIncludes");
	public static final Property rangeIncludes = ResourceFactory.createProperty(BASE_URI + "rangeIncludes");
	public static final Property inverseOf = ResourceFactory.createProperty(BASE_URI + "inverseOf");
	public static final Property name = ResourceFactory.createProperty(BASE_URI + "name");
	public static final Property description = ResourceFactory.createProperty(BASE_URI + "description");
	
    /**
     * Rules:
     *  - DataType is replaced by rdfs:Datatype
     *  - Schema.org's data types are mapped to XSD
     *  - if a range includes both Text and another class, Text is removed
     *  - the same applies to URL (subclass of Text)
     *  - if a range includes Text and URL only, range becomes xsd:anyURI
     *  - if a range includes URL only, it is replaced by rdfs:Resource
     *  
     * @param schemaorg the original schema.org model
     * @return a copy of schemaorg after having applied the rules described above.
     */
	public static Model toXSD(Model schemaorg) {
		Model xsd = ModelFactory.createDefaultModel();
		Model toRemove = ModelFactory.createDefaultModel();
		
		// process data types: schema:Boolean, schema:Text, schema:URL, schema:Number,
		// schema:Float, schema:Integer, schema:Date, schema:DateTime, schema:Time
		
		StmtIterator datatypes = schemaorg.listStatements(null, RDF.type, DataType);
		while (datatypes.hasNext()) {
			Statement st = datatypes.next();
			toRemove.add(st);
			xsd.add(st.getSubject(), RDF.type, RDFS.Datatype);
		}
		
		StmtIterator booleans = schemaorg.listStatements(null, rangeIncludes, Boolean);
		while (booleans.hasNext()) {
			Statement st = booleans.next();
			toRemove.add(st);
			xsd.add(st.getSubject(), rangeIncludes, XSD.xboolean);
		}
		
		StmtIterator strings = schemaorg.listStatements(null, rangeIncludes, Text);
		while (strings.hasNext()) {
			Statement st = strings.next();
			toRemove.add(st);
			
			Resource p = st.getSubject();
			Set<Statement> range = p.listProperties(rangeIncludes).toSet();
			if (range.size() == 1) {
				xsd.add(p, rangeIncludes, XSD.xstring);
			} else if (range.size() == 2 && range.contains(Text) && range.contains(URL)) {
				xsd.add(p, rangeIncludes, XSD.anyURI);
			}
		}
		
		StmtIterator urls = schemaorg.listStatements(null, rangeIncludes, URL);
		while (urls.hasNext()) {
			Statement st = urls.next();
			toRemove.add(st);
			
			Resource p = st.getSubject();
			Set<Statement> range = p.listProperties(rangeIncludes).toSet();
			if (range.size() == 1) {
				xsd.add(p, rangeIncludes, RDFS.Resource);
			}
		}
		
		StmtIterator numbers = schemaorg.listStatements(null, rangeIncludes, Number);
		while (numbers.hasNext()) {
			Statement st = numbers.next();
			toRemove.add(st);
			xsd.add(st.getSubject(), rangeIncludes, XSD.decimal);
			xsd.add(st.getSubject(), rangeIncludes, XSD.xfloat);
			xsd.add(st.getSubject(), rangeIncludes, XSD.xdouble);
		}
		
		StmtIterator floats = schemaorg.listStatements(null, rangeIncludes, Float);
		while (floats.hasNext()) {
			Statement st = floats.next();
			toRemove.add(st);
			xsd.add(st.getSubject(), rangeIncludes, XSD.xfloat);
			xsd.add(st.getSubject(), rangeIncludes, XSD.xdouble);
		}
		
		StmtIterator decimals = schemaorg.listStatements(null, rangeIncludes, Integer);
		while (decimals.hasNext()) {
			Statement st = decimals.next();
			toRemove.add(st);
			xsd.add(st.getSubject(), rangeIncludes, XSD.decimal);
		}
		
		StmtIterator dates = schemaorg.listStatements(null, rangeIncludes, Date);
		while (dates.hasNext()) {
			Statement st = dates.next();
			toRemove.add(st);
			xsd.add(st.getSubject(), rangeIncludes, XSD.date);
		}
		
		StmtIterator datetimes = schemaorg.listStatements(null, rangeIncludes, DateTime);
		while (datetimes.hasNext()) {
			Statement st = datetimes.next();
			toRemove.add(st);
			xsd.add(st.getSubject(), rangeIncludes, XSD.dateTime);
		}
		
		StmtIterator times = schemaorg.listStatements(null, rangeIncludes, Time);
		while (times.hasNext()) {
			Statement st = times.next();
			toRemove.add(st);
			xsd.add(st.getSubject(), rangeIncludes, XSD.time);
		}
		
		toRemove.add(schemaorg.listStatements(Boolean, null, (RDFNode) null));
		toRemove.add(schemaorg.listStatements(Text, null, (RDFNode) null));
		toRemove.add(schemaorg.listStatements(URL, null, (RDFNode) null));
		toRemove.add(schemaorg.listStatements(Number, null, (RDFNode) null));
		toRemove.add(schemaorg.listStatements(Float, null, (RDFNode) null));
		toRemove.add(schemaorg.listStatements(Integer, null, (RDFNode) null));
		toRemove.add(schemaorg.listStatements(Date, null, (RDFNode) null));
		toRemove.add(schemaorg.listStatements(DateTime, null, (RDFNode) null));
		toRemove.add(schemaorg.listStatements(Time, null, (RDFNode) null));

		return xsd.add(schemaorg.difference(toRemove));
	}

	/**
	 * rangeIncludes and domainIncludes are emulated by artificial super-classes.
	 * Based on toXSD.
	 * 
	 * @param schemaorg the original schema.org model
	 * @return a copy of schemaorg using RDFS vocabulary only
	 */
	public static Model toRDFS(Model schemaorg) {
		return toRDFS(schemaorg, getURI());
	}
	
	/**
	 * rangeIncludes and domainIncludes are emulated by artificial super-classes.
	 * Based on toXSD.
	 * 
	 * @param schemaorg the original schema.org model
	 * @param namespace if the vocabulary namespace is not schema.org
	 * @return a copy of schemaorg using RDFS vocabulary only
	 */
	public static Model toRDFS(Model schemaorg, String namespace) {
    	Model rdfs = ModelFactory.createDefaultModel();
    	
    	Model xsd = toXSD(schemaorg);

    	// process classes: rdf:type, rdfs:subClassOf

    	StmtIterator classes = xsd.listStatements(null, RDF.type, RDFS.Class);
    	while (classes.hasNext()) {
    		Resource c = classes.next().getSubject();

    		if (!isDatatype(c)) {
        		rdfs.add(c, RDF.type, RDFS.Class);
        		
        		StmtIterator statements = c.listProperties(RDFS.subClassOf);
        		rdfs.add(statements.toList());
        		
        		StmtIterator enumeration = xsd.listStatements(null, RDF.type, c);
    			rdfs.add(enumeration.toList());
    		}
    	}

    	// process properties: rdf:type, rdfs:subPropertyOf,
    	// schema:domainIncludes, schema:rangeIncludes
    	
    	StmtIterator properties = xsd.listStatements(null, RDF.type, RDF.Property);
    	while (properties.hasNext()) {
    		Resource p = properties.next().getSubject();
    		rdfs.add(p, RDF.type, RDF.Property);
    		
    		List<Resource> domain = new ArrayList<Resource>();
    		List<Resource> range = new ArrayList<Resource>();
    		
    		StmtIterator statements = p.listProperties();
    		while (statements.hasNext()) {
    			Statement st = statements.next();
    			
    			if (st.getPredicate().equals(RDFS.subPropertyOf)) {
    				rdfs.add(p, RDFS.subPropertyOf, st.getResource());
    			} else if (st.getPredicate().equals(domainIncludes)) {
    				domain.add(st.getResource());
    			} else if (st.getPredicate().equals(rangeIncludes)) {
    				range.add(st.getResource());
    			}
    		}
    		
			ClassSet domainSet = new ClassSet(domain, namespace);
    		Resource domainUnion = domainSet.get();
    		rdfs.add(p, RDFS.domain, domainUnion);
    		rdfs.add(domainUnion, RDF.type, isDatatype(domainUnion) ? RDFS.Datatype : RDFS.Class);
    		for (Resource c : domainSet.getSubClasses()) {
    			rdfs.add(c, RDFS.subClassOf, domainUnion);
    		}

			ClassSet rangeSet = new ClassSet(range, namespace);
    		Resource rangeUnion = rangeSet.get();
    		rdfs.add(p, RDFS.range, rangeUnion);
    		rdfs.add(rangeUnion, RDF.type, isDatatype(rangeUnion) ? RDFS.Datatype : RDFS.Class);
    		for (Resource c : rangeSet.getSubClasses()) {
    			rdfs.add(c, RDFS.subClassOf, rangeUnion);
    		}
    	}
    	
    	// TODO keep RDFS annotation properties (label, comment)

    	return rdfs;
	}

	/**
	 * Based on the RDFS transformation, with following additions:
	 * 
	 * Properties are either redefined as object properties or datatype
	 * properties and inverse property assertions are added.
	 * 
	 * Domains and ranges are defined as exclusive unions (owl:unionOf)
	 * of disjoint classes and enumerations are rewritten as enumerated
	 * classes (owl:oneOf).
	 * 
	 * @param schemaorg the original schema.org model
	 * @return a restrictive OWL ontology based on schemaorg
	 */
	public static Model toOWL(Model schemaorg) {
		return toOWL(schemaorg, getURI());
	}
    
	/**
	 * Based on the RDFS transformation, with following additions:
	 * 
	 * Properties are either redefined as object properties or datatype
	 * properties and inverse property assertions are added.
	 * 
	 * Domains and ranges are defined as exclusive unions (owl:unionOf)
	 * of disjoint classes and enumerations are rewritten as enumerated
	 * classes (owl:oneOf).
	 * 
	 * @param schemaorg the original schema.org model
	 * @param namespace if the vocabulary namespace is not schema.org
	 * @return a restrictive OWL ontology based on schemaorg
	 */
    public static Model toOWL(Model schemaorg, String namespace) {
    	Model owl = ModelFactory.createDefaultModel();
    	
    	Model rdfs = toRDFS(schemaorg, namespace);

		// process classes: rdf:type, owl:disjointUnionOf, owl:oneOf
    	
    	StmtIterator classes = rdfs.listStatements(null, RDF.type, RDFS.Class);
    	// note: owl:disjointUnionOf not defined in OWL package?
    	final Property disjointUnionOf = ResourceFactory.createProperty(OWL.getURI(), "disjointUnionOf");
    	while (classes.hasNext()) {
    		Resource c = classes.next().getSubject();

    		owl.add(c, RDF.type, OWL.Class);

    		if (rdfs.contains(null, RDFS.subClassOf, c)) {
    			ResIterator subclasses = rdfs.listResourcesWithProperty(RDFS.subClassOf, c);
	    		RDFList sc = owl.createList(subclasses);
	    		owl.add(c, disjointUnionOf, sc);
    		} else if (rdfs.contains(null, RDF.type, c)) {
    			ResIterator instances = rdfs.listResourcesWithProperty(RDF.type, c);
        		RDFList i = owl.createList(instances);
        		owl.add(c, OWL.oneOf, i);
    		}
    	}
    	
    	// process properties: owl:DatatypeProperty, owl:ObjectProperty, owl:inverseOf
    	
    	ExtendedIterator<Statement> properties = rdfs.listStatements(null, RDF.type, RDF.Property);
    	while (properties.hasNext()) {
    		Resource p = properties.next().getSubject();
    		
    		if (isDatatypeProperty(p)) {
    			owl.add(p, RDF.type, OWL.DatatypeProperty);
    		} else {
    			owl.add(p, RDF.type, OWL.ObjectProperty);
    		}

    		owl.add(p.listProperties(RDFS.domain));
    		owl.add(p.listProperties(RDFS.range));
    		
    		owl.add(p.listProperties(RDFS.subPropertyOf));

    		if (p.hasProperty(inverseOf)) { // FIXME not in rdfs but in schemaorg
    			owl.add(p, OWL.inverseOf, p.getPropertyResourceValue(inverseOf));
    		}
    	}

    	// Holger Knublauch's original mapping:
    	//
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
        // Properties that either allowed numbers or strings have been mapped to xsd:float.
    	// Properties that either allowed xsd:string or URL have been mapped to xsd:anyURI.
    	
        return owl;
    }

    private static boolean isDatatype(Resource type) {
    	return type.getURI().startsWith(XSD.getURI()) ||
    			(type.getModel() != null && type.hasProperty(RDF.type, RDFS.Datatype));
    }
    
    private static boolean isDatatypeProperty(Resource property) {
    	return property.listProperties(RDFS.range).filterKeep(new Predicate<Statement>() {
    		public boolean test(Statement t) {
    			return isDatatype(t.getResource());
    		}
		}).hasNext();
    }
	
	public static String getURI() {
		return BASE_URI;
	}

}
