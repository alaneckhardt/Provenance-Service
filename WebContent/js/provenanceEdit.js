
/**
 * Adds the autocomplete functionality to the specified id.
 * @param id id of edit box
 * @param range range to query - artifact, person, process, ...
 * @param select function what to do with the selected element of the form function(e, ui) { var x = ui.item.id  }
 * @param change function on change. Usually used to clear the edit box.
 */
function addAutocomplete(id, range, select, change){		
		//attach autocomplete  
        $('#'+id).autocomplete({  						  
            //define callback to format results  
            source: function(req, add){  					
			    if(range == null)
			    	range = "http://openprovenance.org/ontology#Artifact";   
                //pass request to server  
                $.get("/ourspaces/search/quicksearch.jsp?type="+escape(range)+"&output=JSON", req, function(data) {
					//Trim the data.
					var data = data.replace(/^\s+|\s+$/g, '') ;				
					var json =  eval('(' + data + ')');
                    //create array for response objects  
                    var suggestions = [];  
                    //process response  
                  	$.each(json, function(i, val){  
                    	suggestions.push(val);  
                		});  
                		//pass array to callback  
                		add(suggestions);  
                });
                
        },  
        create: function(e, ui) {  
			$('.ui-autocomplete.ui-menu ').css("z-index","2000");
        },
        open: function(event, ui) { 
			$('.ui-autocomplete.ui-menu ').css("z-index","2000");
		},
        //define select handler  
        select: select,
        close:function(e, ui) { 
		  	//Empty the edit box
		   	$('#'+id).val("");
        },
	    change : change
     });
        
	
}

function hideSelected() {
	hideType(".selected", "");
}
function showAll(){
	hideType(".selected", "checked");
}

function initProvenance() { 

	   loadProperties();
	   loadSuperclassesProcess();
	   loadSuperclassesArtifact();
	   loadSuperclassesAgent();
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

		$("#ArtifactsList").hide();
		$("#AgentsList").hide();
		$("#ProcessesList").hide();

		$("#ProcessesDisableList").hide();
		$("#ArtifactsDisableList").hide();
		$("#AgentsDisableList").hide();
		
		$("#ArtifactsList").treeview({collapsed : true});
		$("#AgentsList").treeview({collapsed : true});
		$("#ProcessesList").treeview({collapsed : true});

		$("#ProcessesDisableList").treeview({collapsed : true});
		$("#ArtifactsDisableList").treeview({collapsed : true});
		$("#AgentsDisableList").treeview({collapsed : true});
		
		$("#center-container").resizable({maxWidth: 945});
		
		addAutocomplete('provenanceInputString', $("#classSelect").val(), function(e, ui) {          
					//Empty the edit box
					//TODO check what this is doing exactly $('#'+name).val("");
		      //Add the resource to the graph
		      	var query = "/ProvenanceService/ProvenanceService?action=getNode&resource="+escape(ui.item.id);
		      	$.get(query, function(data) {
		      		//Trim the data.
		      		var data = data.replace(/^\s+|\s+$/g, '') ;
		      		graph =  eval('(' + data + ')');
		      		for(x in graph){
		      			var node = graph[x];
		      			try{
		      				var d = createElement(node);
		      				if(d!=null)
								shrinkDiv(d, zoomLevel/10, jsPlumb.offsetX+jsPlumb.width/2, jsPlumb.offsetY+jsPlumb.height/2);
		    				
		      			}
		      			catch(err)
		      			  {
		      			  	alert(err);
		      			  }
		      		}
		      	});	              
		    });

		//Add possibility to disable edges
		for(x in edges){
			var edge = edges[x];
			var div = $("<div>");
			var el = $("<input>");
			el.attr("type","checkbox");
			el.attr("data-edge",x);
			el.attr("checked","checked");
			div.append(el);
			div.append(edge.edge);
			el.click(function() {
					if($(this).attr("checked")=="checked"){
						var edge = edges[$(this).attr("data-edge")];
						//Show canvas
						$('[data-type="'+edge.idEdge+'"]').show();
						//Show div with label
						$('#infovis :contains("'+edge.edge+'")').show();
						
					}
					else{
						var edge = edges[$(this).attr("data-edge")];
						$('[data-type="'+edge.idEdge+'"]').hide();
						$('#infovis :contains("'+edge.edge+'")').hide();						
					}
			});
			div.appendTo("#edgesDisable");
		}
		
		//Add checkbox to ProcessDisable list

		$("#ProcessesDisableList li,#ArtifactsDisableList li,#AgentsDisableList li ").each(function(index) {
			//Only one checkbox.
			if($(this).children("a").children("input").html() == null){
				var check = $("<input>");
				check.attr("type","checkbox");
				check.attr("checked","checked");
				//No need to have click event - all is done in the parent "a href". See hideType reference.
			  /*check.click(function() {
				  var selector = '[data-fulltype="'+$(this.parentNode.parentNode).attr("data-class")+'"]';
					var checked = $(this).attr("checked");
					hideType(selector, checked);
			  });*/
			  //Append the checkbox just after the link.
				$(this).children("a").append(check);
			}
		});
		
	}
	
	function hideType(selector, checked){
		$(selector).each(function(index) {
			if(checked != "checked"){
				//Hide the div
				$(this).hide();
				//Hide endpoints  and edges
				var id = $(this).attr("id");
				$("."+id).hide();
				json[$(this).attr("data-node")].hidden = true;
			}
			else{
				//Show the div
				$(this).show();
				//Show endpoints and edges
				var id = $(this).attr("id");
				$("."+id).show();
				json[$(this).attr("data-node")].hidden = false;
				var c = jsPlumb.getConnections({source:id});  
				c = c.concat(jsPlumb.getConnections({target:id}));  
				for(var con in c){
					var connection = c[con];
					connection.repaint();
				}
			}
		});
	}
	
	function uncheck(el){
		var ch = $(el).children('input'); 
		if(ch.attr('checked')=='checked') {
			ch.removeAttr('checked');
		}
		else{
			ch.attr('checked','checked');
		}
		var type = $(ch).parent().parent().attr("data-class");
		//Call the event explicitly.
	    var selector = '[data-fulltype="'+type+'"]';
		var checked = ch.attr("checked");
		hideType(selector, checked);
		var index = disabledTypes.indexOf(type); 
		if(index==-1){
			//Add new disabled type.
			disabledTypes.push(type);
		}
		else{
			//Delete the type from the array.
			disabledTypes.splice(index,1);
		}
	}