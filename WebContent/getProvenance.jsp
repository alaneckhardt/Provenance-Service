<%@ page language="java" import="provenanceService.*,com.hp.hpl.jena.rdf.model.*, com.hp.hpl.jena.ontology.*,java.util.Iterator,java.util.*,java.net.*,java.text.SimpleDateFormat,java.util.ArrayList,java.io.*,java.net.*,java.util.Vector" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<% 

  ParameterHelper.setSess(session); 
	ParameterHelper.setReq(request);

	String entityId = (String)ParameterHelper.getParameter("entity",  "");	
	Graph g = ProvenanceService.getProvenance(entityId);
	String json = ProvenanceService.graphToJSONString(g);
%>
<%=json %>