<%@ page language="java" import="provenanceService.*, com.hp.hpl.jena.rdf.model.*, com.hp.hpl.jena.ontology.*,java.util.Iterator,java.util.*,java.net.*,java.text.SimpleDateFormat,java.util.ArrayList,java.io.*,java.net.*,java.util.Vector" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
	
	/**
	 * This is a class for containing the properties.
	 */
	function Edge() {
		//Uri of the edge
		this.idEdge = 'prop';
		//Name to display
	    this.edge = 'prop';
	    this.causeAllValuesFrom = 'http://openprovenance.org/ontology#Agent';
	    this.effectAllValuesFrom = 'http://openprovenance.org/ontology#Agent';
	}

	function loadProperties(){
		var edge;
		//Reset the restrictions for empty array.
		edges = [];
    edge = new Edge(); edge.idEdge = 'http://openprovenance.org/ontology#WasControlledBy'; edge.edge = 'WasControlledBy'; edge.causeAllValuesFrom = 'http://openprovenance.org/ontology#Agent'; edge.effectAllValuesFrom = 'http://openprovenance.org/ontology#Process'; edges[0] = edge; edge = new Edge(); edge.idEdge = 'http://openprovenance.org/ontology#WasTriggeredBy'; edge.edge = 'WasTriggeredBy'; edge.causeAllValuesFrom = 'http://openprovenance.org/ontology#Process'; edge.effectAllValuesFrom = 'http://openprovenance.org/ontology#Process'; edges[1] = edge; edge = new Edge(); edge.idEdge = 'http://openprovenance.org/ontology#Used'; edge.edge = 'Used'; edge.causeAllValuesFrom = 'http://openprovenance.org/ontology#Artifact'; edge.effectAllValuesFrom = 'http://openprovenance.org/ontology#Process'; edges[2] = edge; edge = new Edge(); edge.idEdge = 'http://openprovenance.org/ontology#WasGeneratedBy'; edge.edge = 'WasGeneratedBy'; edge.causeAllValuesFrom = 'http://openprovenance.org/ontology#Process'; edge.effectAllValuesFrom = 'http://openprovenance.org/ontology#Artifact'; edges[3] = edge; edge = new Edge(); edge.idEdge = 'http://openprovenance.org/ontology#WasDerivedFrom'; edge.edge = 'WasDerivedFrom'; edge.causeAllValuesFrom = 'http://openprovenance.org/ontology#Artifact'; edge.effectAllValuesFrom = 'http://openprovenance.org/ontology#Artifact'; edges[4] = edge; 
		<% 
		//TODO - load edges from database 
		// TODO - fix the ontology so that it contains the OPM directly 
			int count = 0;	
			Vector<String> subclasses = RDFProvider.getSubclasses("http://openprovenance.org/ontology#Edge");
			for(int i = 0;i<subclasses.size();i++){
				String c = subclasses.get(i);
				//Adding subclasses of this edge.
				subclasses.addAll(RDFProvider.getSubclasses( c));
				Iterator<Restriction> it = RDFProvider.getRestrictionsOnClass(c);
				
				String causeAllValuesFrom = "";
				String effectAllValuesFrom = "";
				while (it.hasNext()) {
					Restriction rest = it.next();			
					if(rest.isAllValuesFromRestriction()){
						Resource r = rest.asAllValuesFromRestriction().getAllValuesFrom();
						Property p = rest.getOnProperty();
						if(p.getLocalName().equals("cause"))
							causeAllValuesFrom = r.getURI();
						else if(p.getLocalName().equals("effect"))
							effectAllValuesFrom = r.getURI();
					}//End if
					
				}//End while
				%>
					edge = new Edge();	
					edge.idEdge = '<%=c %>';
				    edge.edge = '<%=c.substring(c.indexOf('#')+1) %>';
				    edge.causeAllValuesFrom = '<%=causeAllValuesFrom %>';
				    edge.effectAllValuesFrom = '<%=effectAllValuesFrom %>';			    
					edges[<%=count%>] = edge;
									
				<%
				
				count++; }//End for	
	%>
	}