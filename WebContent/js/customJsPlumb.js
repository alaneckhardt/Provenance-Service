var zoomLevel = 10;
function wheel(event){
    var delta = 0;
    if (!event) /* For IE. */
            event = window.event;
    if (event.wheelDelta) { /* IE/Opera. */
            delta = event.wheelDelta/120;
    } else if (event.detail) { /** Mozilla case. */
            /** In Mozilla, sign of delta is different than in IE.
             * Also, delta is multiple of 3.
             */
            delta = -event.detail/3;
    }
    /** If delta is nonzero, handle it.
     * Basically, delta is now positive if wheel was scrolled up,
     * and negative, if wheel was scrolled down.
     */
    if (delta){
    	var multiple = 1.5;
    	if(delta<0)
    		multiple = 1/1.5;
		var centerX = event.pageX - $("#infovis").offset().left;
		var centerY = event.pageY - $("#infovis").offset().top;
		//Limit the zooming.
		if((zoomLevel > 30 && multiple > 1 )||( zoomLevel < 3 && multiple < 1))
			return;
		zoomLevel*=multiple;

		shrinkEdges();
    	/*$('._jsPlumb_endpoint').each(function(index) { 
    		jsPlumb.repaint($(this));//Everything
    	});*/
    	
    	$('.shape').each(function(index) {   
    		shrinkDiv(this, multiple, centerX, centerY);    		
    	});

		//Repaint correct position of the draggable endpoints.
		jsPlumb.repaintEverything();
    }
    /** Prevent default actions caused by mouse wheel.
     * That might be ugly, but we handle scrolls somehow
     * anyway, so don't bother here..
     */
    if (event.preventDefault)
            event.preventDefault();
    event.returnValue = false;	
}
/**
 * @param sessionId id of the session
 */
function loadSession(sessionId){
		var query = server+"ProvenanceService?action=getGraph&session="+sessionId;
			$.get(query, function(data) {
				//Trim the data.
				var data = data.replace(/^\s+|\s+$/g, '') ;
				var graph =  eval('(' + data + ')');
				for(x in graph){
					var node = graph[x];
					createElement(node);
				}
				for(x in graph){
					var node = graph[x];
					for(y in node.adjacencies){
						var adj = node.adjacencies[y];		
						displayRelationship(adj.id,adj.type, adj.from, adj.to);
					}
				}
				layout();
			});
}

	$(document).ready(function(){
		if (document.getElementById("infovis").addEventListener){
			var mousewheelevt=(/Firefox/i.test(navigator.userAgent))? "DOMMouseScroll" : "mousewheel" //FF doesn't recognize mousewheel as of FF3.x
				 
				if (document.getElementById("infovis").attachEvent) //if IE (and Opera depending on user setting)
					document.getElementById("infovis").attachEvent("on"+mousewheelevt, wheel)
				else if (document.getElementById("infovis").addEventListener) //WC3 browsers
					document.getElementById("infovis").addEventListener(mousewheelevt, wheel, false)
				    
			
		   // window.addEventListener('DOMMouseScroll', wheel, false);
		}
		$("#infovis").draggable({
			stop : function() {
				//Repaint correct position of the draggable endpoints.
				jsPlumb.repaintEverything();
			}
	});
	});
	function initJsPlumb() { 	
		jsPlumb.reset();
		jsPlumb.ready(function() {	
			jsPlumb.Defaults.PaintStyle = {
					lineWidth:4,
					strokeStyle : "#aaa"
				};
			jsPlumb.Defaults.Endpoint = [ "Dot", { radius:10	 }, { isSource:true, isTarget:true}];
			jsPlumb.Defaults.MaxConnections = 10;
			jsPlumb.Defaults.Container = $("#infovis");
			
		});
		//loadSession(sessionId);


		//Panning support
		jsPlumb.draggable($(".agent"));
		jsPlumb.draggable($(".artifact"));
		jsPlumb.draggable($(".process"));
		$(".artifact").draggable();
	}
	
	/**
	 * Displays given node
	 * @param id URI of the node
	 * @param title Title
	 * @param shape Shape of the node
	 * @param opmType URI of the type.
	 */
	function displayEntity(id, title, basicType, fullType){		
			var escId = getSimpleId(id);
			var jsonnode = {
				"id": id,
				"title": title,
				"basicType": basicType,
				"fullType": fullType,
				"escId":escId,
				"adjacencies":[]
			};			
			createElement(jsonnode);
	}
	
	/**
	 * 
	 * @param id URI of the edge
	 * @param type Type of the connection
	 * @param from URI of the source div
	 * @param to URI of the target div
	 */
	function displayRelationship(id,type, from, to){
		
		//Trim the data.
		var idTrim = id.replace(/^\s+|\s+$/g, '') ;
		var typeVis = type.substring(type.indexOf('#')+1);		
		var escfrom = from.substring(from.indexOf('#')+1);		
		var escto = to.substring(to.indexOf('#')+1);
		
		//Do not duplicate edges.
		if(checkExistsEdge(idTrim, escfrom, escto))
			return;

		setClasses(escfrom+" "+escto+" _jsPlumb_overlay");
		//var fromJson = json[$("#"+escfrom).attr("data-node")];
		//fromJson.adjacencies.push({"from":, "to":$("#"+escto).attr("data-node"), "title":title});
		var loc = 0.5;
		//Hack for allowing two names of edges above one another.
		if("Used"==typeVis)
			loc = 0.4;
		//	anchor:"AutoDefault",
		var anchors = [[0.5, 0, 0, -1], [1, 0.5, 1, 0], [0.5, 1, 0, 1], [0, 0.5, -1, 0] ];
		var text = typeVis;
		if(typeof provenanceEditable  != "undefined" && provenanceEditable == true){
			text+=" <span class=\"delete\"><a href=\"#\" onclick=\"removeEdge('"+idTrim+"','"+escfrom+"','"+escto+"');\">x</a></span>";
		}
		var con = jsPlumb.connect({
			source:escfrom,
			target:escto,
			dynamicAnchors:anchors,	
			connector:[ "Straight" ],//[ "Straight"], ["Flowchart"]
			overlays:[ 
			   [ "Arrow", { width:15, length:15, location:1, cssClass:"arrow" }], 
			   [ "Label",  {
				   			id:idTrim,
				   			label:text, 
				   			cssClass:"label", 
				   			location:loc,
				   			labelStyle:{ color : "black" } } ]
			],
			endpoint:["Rectangle",{ width:5, height:3, isSource:false, isTarget:false}]
		});
		$(con.canvas).attr("data-id",idTrim);
		$(con.canvas).attr("data-type",type);
		$(con.canvas).attr("data-typetext",typeVis);
	}
	
	/**
	 * Removes the node.
	 * @param node JSON object
	 */
	function removeElement(nodeId){
		var r=confirm("Really delete the node and all its connections?");
		if (r==true)
		{
			removeNode(nodeId);
			jsPlumb.removeAllEndpoints(getSimpleId(nodeId));			
			var e = $('#'+getSimpleId(nodeId));
			//Set json to empty object
			json[e.attr("data-node")] = new Object();
			//Delete the element from canvas
			e.remove();		
			//TODO remove all connections
		}
	}

	/**
	 * Removes the connection.
	 * @param id URI of the edge
	 * @param sourceId HTML-id of the source div
	 * @param targetId HTML-id of the target div
	 */
	function removeEdge(id, sourceId, targetId){
		var r=confirm("Really delete the connection?");
		if (r==true)
		{
			removeCausalRelationship(id);
			var c = jsPlumb.getConnections({source:sourceId,target:targetId});  
			jsPlumb.detach(sourceId, targetId);
		}
	}

	
	function findEdges(cause, effect, edgesIn){
		var x;
		var res = [];		
		for(x in edgesIn){
			var edge = edges[x];
			if(effect == null && (edge.causeAllValuesFrom==cause || (superclasses[cause] != null && $.inArray(edge.causeAllValuesFrom, superclasses[cause]) != -1) )){
				res.push(edge);
			}
			else if(cause == null && (edge.effectAllValuesFrom==effect ||(superclasses[effect] != null && $.inArray(edge.effectAllValuesFrom, superclasses[effect]) != -1))){
				res.push(edge);
			}
			else if(cause != null && effect != null 
					&& (edge.causeAllValuesFrom==cause || (superclasses[cause] != null && $.inArray(edge.causeAllValuesFrom, superclasses[cause]) != -1))					
					&& (edge.effectAllValuesFrom==effect|| (superclasses[effect] != null && $.inArray(edge.effectAllValuesFrom, superclasses[effect]) != -1))){
				res.push(edge);
			}
		}
		return res;
	}
	
	function checkExists(id){
		for(var x in json){
			var o = json[x];
			if(id == o.id)
				return true;
		}
		return false;
	}

	function checkExistsEdge(id, from, to){
		var con = jsPlumb.getConnections({ source:from, target:to});
		if(con == null)
			return false;
		
		for(var x in con){
			var c = con[x];			
			if($(c.canvas).attr("data-id")==id)
				return true;
		}
		return false;
	}
	
	/**
	 * Shrinks the div with given multiplier
	 * @param div Div to shrink
	 * @param multiple How many times to shrink
	 */
	function shrinkDiv(div, multiple, centerX, centerY){ 		
		var x = $(div).css("left");
		var y = $(div).css("top");
		var w = $(div).width();
		//var h = $(div).height();
		//Strip the px at the end
		x=x.substring(0,x.length-2);
		y=y.substring(0,y.length-2);
		$(div).css("left",(x-centerX)*multiple+centerX);
		$(div).css("top",(y-centerY)*multiple+centerY);
		$(div).width(w*multiple);
		//$(div).height(h*multiple);
		
		// Change font size
	    var currentFontSize = $(div).css('font-size');
	    var currentFontSizeNum = parseFloat(currentFontSize, 10);
	    var newFontSize = currentFontSizeNum*multiple;
	    $(div).css('font-size', newFontSize);
	    
	    //Shrink the endpoint
	    jsPlumb.Defaults.Endpoint = [ "Dot", { radius:zoomLevel	 }, { isSource:true, isTarget:true}];			
	    var endpoint = jsPlumb.getEndpoint("end-"+$(div).attr("id"));
		w = $(endpoint.canvas).width();
		endpoint.setPaintStyle({radius:zoomLevel, fillStyle:"#aaa"});
		
		//Shrink the trigger icon
		w = $(div).children(".trigger").width();
		$(div).children(".trigger").width(w*multiple);
		$(div).children(".trigger").height(w*multiple);
		//Shrink the info icon
		w = $(div).children(".info").width();
		$(div).children(".info").width(w*multiple);
		$(div).children(".info").height(w*multiple);
		
		//Shift the controls to the right
		$(div).children(".controls").css("left",$(div).width());
		$(div).children(".controls").css("top","-"+$(div).height());
		
		//jsPlumb.repaint($(div));//Everything
		//jsPlumb.repaint(endpoint);//Everything
	}
	
	/**
	 * Shrinks the edge label with given multiplier
	 * @param edge Label to shrink
	 * @param multiple How many times to shrink
	 * */
	function shrinkEdges(){
		// Change font size
	    var currentFontSize = $("._jsPlumb_overlay.label").css('font-size');
	    var currentFontSizeNum = parseFloat(currentFontSize, 10);
	    var newFontSize = 14*zoomLevel/10;
	    $("._jsPlumb_overlay.label").css('font-size', newFontSize);	    
	}
	
	function loadProvenance(res){
		var query = serverVisual+"getProvenance.jsp?entity="+escape(res);
		$.get(query, function(data) {
			//Trim the data.
			var data = data.replace(/^\s+|\s+$/g, '') ;
			var graph =  eval('(' + data + ')');
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
			for(x in graph){
				var node = graph[x];
				for(y in node.adjacencies){
					var adj = node.adjacencies[y];		
					displayRelationship(adj.id,adj.type, adj.from, adj.to);
				}
			}
			shrinkEdges();
			jsPlumb.repaintEverything();

			$('.info').hover(function() {
				 $(this).css('cursor','pointer');
				 }, function() {
				 $(this).css('cursor','auto');
				});
			$('.trigger').hover(function() {
				 $(this).css('cursor','pointer');
				 }, function() {
				 $(this).css('cursor','auto');
				});
			
			//layout();
			initProvDiplay();			
		});
	}
	   
	function getElementDiv(node){
		var dInner = $('<div>');
		var dTrigger = $('<div>');
		var dIcon = $('<div>');
		var dInfo = $('<div>');
		dInner.attr("id", getSimpleId(node.id));
		dInner.addClass(node.basicType);
		dInner.addClass("shape");
		dInner.css("z-index","4");
		dInner.html("<p style=\"padding: 0.1em 0;\">"+node.title+"</p>");
		var w = jsPlumb.width, h = jsPlumb.height;
		var x = jsPlumb.offsetX + (0.1 * w) + Math.floor(Math.random()*(0.8 * w));
		var y = jsPlumb.offsetY + (0.1 * h) + Math.floor(Math.random()*(0.8 * h));
		dInner.css("top",y + 'px');
		dInner.css("left", x + 'px');	

		//Controls removed - TODO - delete commented block
		/*var dControls = $('<div>');
		dControls.addClass("controls");
		dControls.append("<a href=\"#\" onclick=\"removeElement('"+node.id+"')\">Delete</a><br>");
		dControls.append("<a href=\"#\" onclick=\"loadProvenance('"+node.id+"')\">Load provenance</a><br>");
		
	   
			   
		dControls.attr("id", getSimpleId(node.id)+"controls");
		dInner.append(dControls);
		//Set the onmouse events for popup of controls.
		  // Allow mouse over of details without hiding details
		  $(dControls).mouseover(function()
		  {		  
		    if (hideTimer)
		      clearTimeout(hideTimer);
		      dControls.css('opacity', '1');
		      dControls.css('z-index', '2000');
		      dInner.css('z-index', '2000');
		  });
		  $(dControls).mouseout(function()
			{
				if (hideTimer)
				   clearTimeout(hideTimer);
			  dControls.css('opacity', '0.5');
			});
		
		  
	
		  $(dInner).mouseout(function()
		  {
		      if (hideTimer)
		          clearTimeout(hideTimer);
		      hideTimer = setTimeout(function()
		      {
		          dControls.css('display', 'none');
		          dControls.css('z-index', '0');
			      dInner.css('z-index', '4');
		      }, hideDelay);
		  });	
		*/
		//Popup for trigger.
		  $(dTrigger).qtip({
		  	    content: "Load the provenance around this node.",
			    show:{
			       delay:1500
			    },
			    hide: {
			       delay:500,
		           fixed: true // Make it fixed so it can be hovered over
		        },
		       position: { adjust: { x: -10, y: -10 } },
			   style: {
		           padding: '2px', // Give it some extra padding
		           //name: 'cream',
		           tip: { color: 'black' } 
		        }
			});
		  $(dTrigger).click(function(){
			  loadProvenance(node.id);
			  return false;
		  });
		dTrigger.addClass("trigger");
		
		//Redirect to artifact space
		dInfo.addClass("info");
		
		
		//Adding details at the end - we need to call ajax, which results in details being the last in the list.
	   if(node.basicType == "Agent"){
			//get the person id
			$.get("/ourspaces/rest/person/"+getSimpleId(node.id), function(data) {
				//Trim the data.
				data = data.replace(/^\s+|\s+$/g, '') ;
				var person =  eval('(' + data + ')');
				dInfo.click(function() {
					window.location.href = "./profile.jsp?id="+person.id;
				});
				  $(dInfo).qtip({
				  	    content: "Show details about the person.",
					    show:{delay:1500},
					    hide: {delay:500,fixed: true},
				        position: { adjust: { x: -10, y: -10 } },
					    style: {padding: '2px', tip: { color: 'black' } }
					});
			});
		}
	   //Artifact
		else if(node.basicType == "Artifact"){
			dInfo.click(function() {
				window.location.href = "./artifact_new.jsp?id="+escape(getSimpleId(node.id));
			});
			$(dInfo).qtip({
		  	    content: "Show details about the artifact.",
			    show:{delay:1500},
			    hide: {delay:500,fixed: true},
		        position: { adjust: { x: -10, y: -10 } },
			    style: {padding: '2px', tip: { color: 'black' } }
			});
		}
	   //Process - do not show the link at all
		else{
			dInfo.removeClass("info");
		}
		dIcon.attr("rel",encodeURIComponent(node.id))		
		dIcon.addClass('icon');
		//Do not allow provenance of Agents, there's too much
		if(node.basicType != "Agent"){
			dInner.append(dTrigger);	
		}
		dInner.append(dIcon);	
		dInner.append(dInfo);	
		

		var hideTimer = null;
		var hideDelay = 500;

		
			  
			 /* 
			 dTrigger.hide();	
			 $(dInner).mouseover(function()
			  {
	        //  var endpoint = jsPlumb.getEndpoint("end-"+getSimpleId(node.id));
					//	$(endpoint.canvas).show();
						$(dTrigger).show();
			  });
			  
			  // Hide after mouseout
			  $(dInner).mouseout(function()
			  {
			      if (hideTimer)
			          clearTimeout(hideTimer);
			      hideTimer = setTimeout(function()
			      {
			          dControls.css('display', 'none');
			          dControls.css('z-index', '');
			        //  var endpoint = jsPlumb.getEndpoint("end-"+getSimpleId(node.id));
							//	$(endpoint.canvas).hide();
								$(dTrigger).hide();
			      }, hideDelay);
			  });*/
		return dInner;
	}
	function disable(edge, obj, type){
		var escId = getSimpleId(obj.id);
		$("#"+escId).droppable("destroy");
	}
	function enable(edge, obj, type){
		//enable dragging
		//var uuid = $("#"+obj.id).attr("data-endpoint");
		var edgeName = edge.edge;
		var escId = getSimpleId(obj.id);
		$("#"+escId+" p").droppable({
			scope: jsPlumb.Defaults.Scope,
			drop: function( event, ui ) {
				//TODO - do the combobox to select the edge, if there is multiple types
				//Create the new connection with the new endpoint		
				var id = -1, dataType;
				if(typeof $(this).attr("data-node") == "undefined" || $(this).attr("data-node") == null){
					id = $(this.parentNode).attr("data-node");
					dataType = $(this.parentNode).attr("data-fullType");
				}
				else{
					id = $(this).attr("data-node");
					dataType = $(this).attr("data-fullType");
				}						
				selected2 =json[id];
				var ed = findEdges(type,dataType, edges);
				selectedEdge = ed[0];
				addCausalRelationship(selected1.id, selected2.id, ed[0].idEdge);
			}
		});
		$("#"+escId).addClass("hover");
		//Display the names of edges next to the node
		var txt = "";
		var edgesToDisplay = findEdges(type,$("#"+escId).attr("data-fullType"), edges);
		var z;
		for(z in edgesToDisplay){
			txt += edgesToDisplay[z].edge+",";
		}
		//Strip the last ','
		txt=txt.substring(0,txt.length-1);
		var div = $("<div>");
		div.addClass("labelDrag");
		div.css("margin-left",$("#"+escId).width()+"px");
		div.css("left",$("#"+escId).css("left"));
		div.css("top",$("#"+escId).css("top"));
		div.css("z-index","1000");
		div.html(txt);
		$("#infovis").append(div);
		
		
		//Set paint style of the endpoint of the node.
		var endpoint = jsPlumb.getEndpoint("end-"+escId);
		endpoint.setPaintStyle({fillStyle:"orange", outlineColor:"black", outlineWidth:1, radius:zoomLevel });
		endpoint.paint();
	}
	/**
	 * Create a div representing the node.
	 * @param node JSON object.
	 * @returns the element div
	 */
	function createElement(node) {
		//Do not duplicate nodes
		if(checkExists(node.id))
			return null;
		
		var d = getElementDiv(node);
		var escId = getSimpleId(node.id);
		$('#infovis').append(d);
		//Draggable endpoint.
		setClasses(escId+" _jsPlumb_endpoint");
		var endpoint = jsPlumb.addEndpoint(d,
		 { anchor:"BottomCenter" },
		 {  
			 	uuid:"end-"+escId,
			 	isSource:true, 
			 	//We don't need drop - the elements are made droppable themselves dynamically based on properties
			 	//isTarget:true,
				connector : "Straight",
				dragAllowedWhenFull:true,
				maxConnections: 20,	
				//TODO check the edges
				dragOptions:{
					scope: jsPlumb.Defaults.Scope,	
					start:function(e, ui) {
					//Repaint needed to address the problem with scrolling the whole page and then dragging
					var cons = jsPlumb.getConnections();
					for(var c in cons){
						var con = cons[c];
						con.repaint();
					}
					selected1 = json[$("#"+this.attributes.elid.value).attr("data-node")];
					var type = $("#"+this.attributes.elid.value).attr("data-fullType");
					var ed = findEdges(type,null, edges);
					var x;
					for(x in ed){
						var edge = ed[x];
						var range = edge.effectAllValuesFrom;
						//TODO list all the nodes and check the range and the type of the node.
						var y;
						for(y in json){
							var obj = json[y];
							//Skip the actual object
							if(obj == selected1)
								continue;
							
							if(obj.fullType==range){
								enable(edge, obj, type);
							}
							else if(superclasses[obj.fullType] != null && $.inArray(range, superclasses[obj.fullType]) != -1){
								enable(edge, obj, type);								
							}
							else{
								disable(edge, obj, type);
							}
						}
					}
					},
					stop:function(e, ui) {
						//Erase all stylings
						var y;
						for(y in json){
							var obj = json[y];
							var endpoint = jsPlumb.getEndpoint("end-"+getSimpleId(obj.id));
							endpoint.setPaintStyle({fillStyle:"#aaa", radius:zoomLevel});
							endpoint.paint();
							$("#"+getSimpleId(obj.id)).removeClass("hover");
						}
						//Delete labels with edge names
						$(".labelDrag").remove();
					}
				}				
		 }); 
		//Make the endpoints for dragging more visible.
		$(endpoint.canvas).css("z-index","5");
		
		var id = getSimpleId(node.id), _d = jsPlumb.CurrentLibrary.getElementObject(d);
		jsPlumb.CurrentLibrary.setAttribute(_d, "id", id);
		jsPlumb.draggable(d);
		 
       
		d.click(function() {
			d.toggleClass("selected");
			//TODO what to do onclick?
		});

		d.attr("data-basicType",node.basicType);
		d.attr("data-title",node.title);
		d.attr("data-id",node.id);
		d.attr("data-fullType",node.fullType);
		d.attr("data-node",json.length);
		
		//Hide it for the start - it will appear when hover above the element.
		//$(endpoint.canvas).hide();
		//Append the new node to the list of nodes.
		json[json.length] = node;
		node.el = d;
		//If not editable, hide the editing things.
		if(provenanceEditable == false){
			//Disable creating new connections.
			//$('._jsPlumb_endpoint').draggable("disable");
			$('._jsPlumb_endpoint').hide();
			//Hide "x" at the edge name.
			$('.delete').hide();
		}
		//Shrink the div
		//shrinkDiv(d, zoomLevel/10, jsPlumb.offsetX+jsPlumb.width/2, jsPlumb.offsetY+jsPlumb.height/2);
		return d;
	};	
	
	/**
	 * Set class to all jsPlumb entities. Useful for getting the canvases back again.
	 * @param id
	 */
	function setClasses(id){
		jsPlumb.connectorClass = id;
		jsPlumb.endpointClass = id;
		jsPlumb.overlayClass = id;
	}
	

	function showEditing(){
		json = jsonBackup;
	}
	
	function collapseEditing(){
		//TODO finish collapsing and showing
		function getOrigin(node){
			var stack = [];
			for(y in node.adjacencies){
				var adj = node.adjacencies[y];	
				var process = null;
				if(adj.type == "http://openprovenance.org/ontology#Used"  && adj.to.type=="http://www.policygrid.org/ourspacesVRE.owl#EditResource"){
					process = adj.from;
				}
				else if(adj.type == "http://openprovenance.org/ontology#WasGeneratedBy"  && adj.to.type=="http://www.policygrid.org/ourspacesVRE.owl#EditResource"){
					process = adj.to;
				}
				if(process != null){
					
				}
			}
			for(var x in json){
				var node2 = graph[x];
				
			}
		}
		jsonBackup = json;
		var collapsed = [];
		for(var x in json){
			var node = graph[x];
			
		}

		for(var x in json){		
			shrinkDiv(d, zoomLevel/10, jsPlumb.offsetX+jsPlumb.width/2, jsPlumb.offsetY+jsPlumb.height/2);

		}


	}