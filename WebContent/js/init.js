	//Set the seed of the random function
	var seed = 123546;
	var selected1 = null, selected2 = null;
	var selectedEdge;
	Math.seedrandom(seed);
	//Add handling of the ctrl key
		ctrlKey = false;
		shiftKey = false;
		metaKey = false;
		$(document).bind('keyup keydown', function(e){ctrlKey = e.ctrlKey} );
		$(document).bind('keyup keydown', function(e){shiftKey = e.shiftKey} );
		$(document).bind('keyup keydown', function(e){metaKey = e.metaKey} );
	//var server = 'http://mrt.esc.abdn.ac.uk:8080/ProvenanceService/';
	//var serverVisual = 'http://mrt.esc.abdn.ac.uk:8080/ourspaces/testProvenance/';
	var server = 'http://localhost:8080/ProvenanceService/';
	var serverVisual = 'http://localhost:8080/ourspaces/testProvenance/';
	var process_counter = 0;
	var agent_counter = 0;
	var artifacts_counter = 0;
	var edges_counter = 0;
	var process;
	var sessionId = '0';
	
	function init(){
		$.getScript("./js/edges.jsp", function(data, textStatus){
				   loadProperties();
				   for(x in edges){
					   edge = edges[x];
					   $("#-menu").append('<option id="'+edge.edge+'" value="'+edge.idEdge+'" selected>'+edge.edge+'</option>');
				   }
		});
		$.getScript("./getSuperclasses.jsp?className=http://openprovenance.org/ontology%23Artifact", function(data, textStatus){
			   loadSuperclassesArtifact();
				$.getScript("./getSuperclasses.jsp?className=http://openprovenance.org/ontology%23Agent", function(data, textStatus){
					   loadSuperclassesAgent();
						$.getScript("./getSuperclasses.jsp?className=http://openprovenance.org/ontology%23Process", function(data, textStatus){
							   loadSuperclassesProcess();
							   //Now fill the subclasses.
							   var x,y;
							   for(x in superclasses){
								   var subclass = x;
								   var xsuperclasses = superclasses[x];
								   for(y in superclasses){
									   var superclass = y;
									   if(subclasses[y] == null){
										   subclasses[y] = [];
									   }
									   subclasses[y].push(x);
								   }
							   }
						});
				});
		});
		

	}
	

	function typesFit(type, requiredType){
		if(type == requiredType)
			return true;
		if(type == null ||  superclasses[type] == null ||  typeof superclasses[type] == 'undefined')
			return false;
		if($.inArray(requiredType, superclasses[type])==-1)
			return false;
		return true;
	}
	/**
	 * Disable edges that do not correspond to current selection.
	 */
	function disableEdges(){
		var cause = "", effect = "";
		if(selected1 != null)
			cause = selected1.data.$opmType;
		if(selected2 != null)
			effect = selected2.data.$opmType;
		var hidden = true;
		$("#-menu").show();
		$('#addCausalRelationship').removeAttr('disabled');

		for(x in edges){
			var e = edges[x];
			//Test if the required cause type is equal to the selected type or its super-type.
			//E.g. if a Process is required, selected can be DataCollection.
			if(typesFit(cause, e.causeAllValuesFrom)&&
					typesFit(effect, e.effectAllValuesFrom)
			   ){
				$("#"+e.edge).removeAttr("disabled");
				$("#"+e.edge).show();
				$("#"+e.edge).select();
				$("#-menu").val(e.idEdge);
				hidden = false;
			}
			else{
				$("#"+e.edge).attr("disabled","disabled");
				$("#"+e.edge).hide();
			}
		}
		if(hidden){
			$("#-menu").hide();
			$("#addCausalRelationship").attr('disabled', 'disabled');		
		}
	}

		 