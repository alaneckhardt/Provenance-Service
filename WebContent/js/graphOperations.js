
		/**
		 * Returns the part of the URI without the namespace
		 * @param uri
		 * @returns
		 */
		function getSimpleId(uri){
			if(uri == null || uri == "")
				return "";
			if(uri.indexOf('#')>0)
				return uri.substring(uri.indexOf('#')+1);
			else
				return uri.substring(uri.indexOf('/')+1);
		}
		/**
		 * 
		 * @param data URI of the process
		 * @param title Title of the process
		 * @param className Type of the process
		 */
		function displayProcess(data, title, className){
			if (title == null) 
				title = "Process "+ process_counter;
			//Trim the data.
			var dataTrim = data.replace(/^\s+|\s+$/g, '') ;
			displayEntity(dataTrim, title, "rectangle", className);
			process_counter++;
		}

		/**
		 * 
		 * @param data URI of the agent
		 * @param title Title of the agent
		 * @param className Type of the agent
		 */
		function displayAgent(data, title, className){	
			if (title == null) 
				title = "Agent "+ agent_counter;		
			//Trim the data.
			var dataTrim = data.replace(/^\s+|\s+$/g, '') ;
			displayEntity(dataTrim, title, "diamond", className);	
			agent_counter++;	
		}
		
		/**
		 * @param data URI of the artifact
		 * @param title Title of the artifact
		 * @param className Type of the artifact
		 */
		function displayArtifact(data, title, className){
			if (title == null) 
				title = "Artifact "+ artifacts_counter;		
			//Trim the data.
			var dataTrim = data.replace(/^\s+|\s+$/g, '') ;
			displayEntity(dataTrim, title, "ellipse", className);	
			artifacts_counter++;	
		}

		
		/**
		 * Displays new relationship - the required data are taken from selected nodes and selected type of property.
		 * @param data URI of the new relationship.
		 */
		function displayNewCausalRelationship(data){
			//Trim the data.
			var dataTrim = data.replace(/^\s+|\s+$/g, '') ;
			displayRelationship(dataTrim, selectedEdge.idEdge, selected1.id,selected2.id);
		}
		

		/**
		 * Creates new session
		 * @param event function to handle new session id
		 * 
		 */
		function startSession(event){
			var query = server+'ProvenanceService?action=startSession';
			$.get(query, event);
		}

		/**
		 * Rollbacks given session
		 * @param session
		 * @param event
		 * 
		 */
		function rollback(session, event){
			var query = server+'ProvenanceService?action=rollback&session='+escape(session);			
			$.get(query, event);
		}
		/**
		 * Commits given session
		 * @param session
		 * @param event
		 */
		function commit(session, event){
			var query = server+'ProvenanceService?action=commit&session='+escape(session);			
			$.get(query, event);
		}
			
		/**
		 * 
		 * @param className Type of the process
		 */
		function addProcess(className){
			var title = $("#-title").val();
			var query = server+'ProvenanceService?action=addProcess&session='+escape(sessionId);
			if(className != null && className != ""){
				query+="&type="+escape(className);
			}
			
			$.get(query, function(data){
				//Trim the data.
				var data = data.replace(/^\s+|\s+$/g, '') ;
				displayProcess(data, title, className);
				query = server+'ProvenanceService?action=addTitle&session='+escape(data)+'&object='+escape(data)+'&title='+escape(title);
				$.get(query, function(data){
				});
			});
		}


		/**
		 * 
		 * @param className Type of the agent
		 */
		function addAgent(className){
			var title = $("#-title").val();
			var query = server+'ProvenanceService?action=addAgent&session='+escape(sessionId);
			if(className != null && className != ""){
				query+="&type="+escape(className);
			}
			$.get(query, function(data){
				//Trim the data.
				var data = data.replace(/^\s+|\s+$/g, '') ;
				displayAgent(data, title, className);
				query = server+'ProvenanceService?action=addTitle&session='+escape(sessionId)+'&object='+escape(data)+'&title='+escape(title);
				$.get(query,function(data){
				});			
			});
		}

		/**
		 * 
		 * @param className Type of the artifact
		 */
		function addArtifact(className){
			var title = $("#-title").val();
			var query = server+'ProvenanceService?action=addArtifact&session='+escape(sessionId);
			if(className != null && className != ""){
				query+="&type="+escape(className);
			}
			$.get(query, function(data){
				//Trim the data.
				var data = data.replace(/^\s+|\s+$/g, '') ;
				displayArtifact(data, title, className);
				query = server+'ProvenanceService?action=addTitle&session='+escape(sessionId)+'&object='+escape(data)+'&title='+escape(title);
				$.get(query,function(data){
				});
			});
		}

		/**
		 * 
		 * @param uri URI of the resource
		 * @param className Type of the resource
		 * @param title Title of the resource
		 */
		function addExistingResource(uri, className, title){
			var query = server+'ProvenanceService?action=addExistingResource&session='+escape(sessionId)+
			'&resource='+escape(uri)+
			'&type='+escape(className);
			if(title != null && title != ""){
				query+="&title="+escape(title);
			}
			$.get(query, function(data){
				query = server+'ProvenanceService?action=getShape&type='+escape(className);
				$.get(query,function(data){
					displayEntity(uri, title, data, className);						
				});
			});
		}


		
			
		/**
		 * Adds causal relationship. The values are taken from UI.
		 * 
		 * @param cause The cause of the relationship
		 * @param effect The effect of the relationship
		 * @param relation The type of the relationship
		 */
		function addCausalRelationship(cause,effect, relation){
			//var cause = selected1.id;
			//var effect = selected2.id;
			//var relation = $("#-menu").val();
			/*if($("#-menu").is(':hidden')){
				alert("No relation selected");
				return;			
			}*/
			if(relation==null || relation=="" || relation == "x"){
				alert("No relation selected");
				return;
			}                
			
			var query = server+"ProvenanceService?action=addCausalRelationship&session="+escape(sessionId)
				+"&cause="+escape(cause)
				+"&effect="+escape(effect)
				+"&relation="+escape(relation);
			$.get(query, displayNewCausalRelationship);
		}
		
		/**
		 * Adds session to the service.
		 */
		function addSession(){
			var query = server+"ProvenanceService?action=startSession";
			$.get(query, function(data) {
				//Trim the data.
				var data = data.replace(/^\s+|\s+$/g, '');
				sessionId = data;			
			});
			
		}

		/**
		 * 
		 * @param relation URI of the relation.
		 */
		function removeCausalRelationship(relation){
			var query = server+"ProvenanceService?action=removeCausalRelationShip&session="+escape(sessionId)+"&relation="+escape(relation);
			$.get(query, function(data) {
				//Trim the data.
				var data = data.replace(/^\s+|\s+$/g, '');
			});			
		}

		/**
		 * @param node URI of the node.
		 */
		function removeNode(node){
			var query = server+"ProvenanceService?action=removeNode&session="+escape(sessionId)+"&node="+escape(node);
			$.get(query, function(data) {
				//Trim the data.
				var data = data.replace(/^\s+|\s+$/g, '');
			});			
		}
		