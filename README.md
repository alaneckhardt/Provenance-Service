# About
Provenance service is a java library that can be used to create/query/update provenance information. Currently, Provenance Service is using OPM, but in future, support for PROV-O will be implemented as well.

It id used by [Provenance Visualisation](https://github.com/alaneckhardt/Provenance-Visualisation) to get/manipulate the data from the server. 

#Documentation 
The basic documentation is on the Github [wiki](https://github.com/alaneckhardt/Provenance-Service/wiki), otherwise contact the author at : a.e@centrum.cz

#Demo 
An example of usage of Provenance Service in a java code:


		//Start the session
		String session = ProvenanceService.startSession()
		ProvenanceService.addRDFGraph(session, model);

		//Create a new process and add a title
		String processId = ProvenanceService.addNode(session, "http://www.policygrid.org/ourspacesVRE.owl#EditResource");

		ProvenanceService.addTitle(session, processId, "Editing process");

		//Edge to the old artifact
		String edge = ProvenanceService.addCausalRelationship(session, OPM.Used.getURI(), processId, oldArtifact);
		//Add timestamp, which is a custom property
		ProvenanceService.addCustomProperty(session, edge, "http://www.policygrid.org/ourspacesVRE.owl#timestamp",""+now);	
		
		//Edge to the controlling agent	
		String edgeAgent = ProvenanceService.addCausalRelationship(session, OPM.WasControlledBy.getURI(), processId, user.getURI());
		ProvenanceService.addCustomProperty(session, edgeAgent, "http://www.policygrid.org/ourspacesVRE.owl#timestamp",""+now);		
				
		//Edge to the new artifact
		String edgeResource = ProvenanceService.addCausalRelationship(session, OPM.WasGeneratedBy.getURI(), newArtifact, processId);
		ProvenanceService.addCustomProperty(session, edgeResource, "http://www.policygrid.org/ourspacesVRE.owl#timestamp",""+now);
		
		//Commit or rollback the session		
		ProvenanceService.rollback(session);
		ProvenanceService.commit(session);




