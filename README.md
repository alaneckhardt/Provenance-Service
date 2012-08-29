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




#Licensing

Provenance service is distributed under BSD-2-clause licensing:

Copyright (c) 2012, Alan Eckhardt All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
