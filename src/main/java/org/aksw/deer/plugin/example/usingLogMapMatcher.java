package org.aksw.deer.plugin.example;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.OwlOntologyLoader;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.ox.krr.logmap2.LogMap2_Matcher;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;


/**
 * 
 * Example of using LogMap's matching facility 
 * 
 * @author Ernesto
 *
 */
public class usingLogMapMatcher {
	

	
	
	OWLOntology onto1;
	OWLOntology onto2;

	OWLOntologyManager onto_manager1;
	
	public usingLogMapMatcher(){
		//Logger.getLogger(usingLogMapMatcher.class.getName()).setLevel(Level.OFF);
		Logger.getRootLogger().setLevel(Level.OFF);
		
		try{
			System.out.print(System.getProperty("user.dir"));
			
			String onto1_iri = "harvard_eagle-i_net_sparqler.nt";
			String onto2_iri = "cdrewu_eagle-i_net_sparqler.nt";
			
			
			
			
			MissingImportHandlingStrategy silent = MissingImportHandlingStrategy.SILENT;
			
			OWLOntologyLoaderConfiguration ontologyLoaderConfiguration = new OWLOntologyLoaderConfiguration();
			ontologyLoaderConfiguration = ontologyLoaderConfiguration.setMissingImportHandlingStrategy(silent);
		
			System.out.println(ontologyLoaderConfiguration);
			
			OWLOntologyManager onto_manager2 = OWLManager.createOWLOntologyManager();
			
			onto_manager1 = OWLManager.createOWLOntologyManager();
			onto_manager1.setOntologyLoaderConfiguration(ontologyLoaderConfiguration);			
			onto_manager2.setOntologyLoaderConfiguration(ontologyLoaderConfiguration);

			
			
			onto1 = onto_manager1.loadOntologyFromOntologyDocument(new File(onto1_iri));
			onto2 = onto_manager1.loadOntologyFromOntologyDocument(new File(onto2_iri));
			
			
			
			
			//onto1 = onto_manager1.loadOntology(IRI.create(onto1_iri));
			//onto2 = onto_manager1.loadOntology(IRI.create(onto2_iri));
			
			
			LogMap2_Matcher logmap2 = new LogMap2_Matcher(onto1,onto2);
		
			
			  Set<String> representativeLabelsForMappings =
			  logmap2.getRepresentativeLabelsForMappings(); System.out.println("Labels : "
			  + representativeLabelsForMappings);
			  
			  
			  
			  
			 
			//Optionally LogMap also accepts the IRI strings as input 
			//LogMap2_Matcher logmap2 = new LogMap2_Matcher(onto1_iri, onto2_iri);
			
			//Set of mappings computed my LogMap
			Set<MappingObjectStr> logmap2_mappings = logmap2.getLogmap2_Mappings();
			
			Iterator<MappingObjectStr> iterator = logmap2_mappings.iterator();
			
			while(iterator.hasNext()) {
				MappingObjectStr next = iterator.next();
				System.out.println("URL 1 : " + next.getIRIStrEnt1());
				System.out.println("URL 2 : " + next.getIRIStrEnt2());
				System.out.println("Structural Mappings : " + next.getStructuralConfidenceMapping());
				System.out.println("Type Mapping : " + next.getTypeOfMapping());
				System.out.println("Confidence : "+ next.getConfidence());
				System.out.println("lexicalConfidnce:" +next.getLexicalConfidenceMapping());
				System.out.println("property:" +  next.isDataPropertyMapping());
				
				
			}
			
			System.out.println("Number of mappings computed by LogMap: " + logmap2_mappings.size());
			
			
			
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		new usingLogMapMatcher();

	}

}