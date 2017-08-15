package org.schema;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDFS;

/**
 * 
 * Models a set of classes defined either as domain or range of the same property.
 * See:
 *  - http://schema.org/domainIncludes
 *  - http://schema.org/rangeIncludes
 *
 * @author Victor Charpenay
 * @creation 15.08.2017
 *
 */
public class ClassSet {

	private final List<Resource> mSet;
	
	private final Resource mResource;
	
	public ClassSet(List<Resource> set) {
		if (set.isEmpty()) {
			mSet = new ArrayList<Resource>();
    		mResource = RDFS.Resource;
		} else if (set.size() == 1) {
			mSet = new ArrayList<Resource>();
			mResource = set.get(0);
		} else {
			mSet = set;
			mResource = ResourceFactory.createResource(SCHEMA.getURI() + getCanonicalName(set));
		}
	}
	
	public Resource get() {
		return mResource;
	}
	
	public List<Resource> getSubClasses() {
		return mSet;
	}
    
    private static String getCanonicalName(List<Resource> resources) {
    	String name = "";
    	
    	resources.sort(new Comparator<Resource>() {
    		public int compare(Resource first, Resource second) {
    			return first.getLocalName().compareTo(second.getLocalName());
    		};
		});
    	Iterator<Resource> it = resources.iterator();
    	while (it.hasNext()) {
    		name += it.next().getLocalName() + (it.hasNext() ? "Or" : "");
    	}
    	
    	return name;
    }
	
}
