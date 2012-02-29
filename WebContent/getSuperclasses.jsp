<%@ page language="java" import="provenanceService.*, com.hp.hpl.jena.ontology.*,java.util.Iterator,java.util.*,java.net.*,java.text.SimpleDateFormat,java.util.ArrayList,java.io.*,java.net.*,java.util.Vector" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<% ParameterHelper.setSess(session); 
	ParameterHelper.setReq(request);

	String className = (String)ParameterHelper.getParameter("className",  "http://openprovenance.org/ontology#Process");	
%>
function loadSuperclasses<%=className.substring(className.indexOf('#')+1) %>(){
	<%
	Vector<String> subClasses = RDFProvider.getSubclasses( className);
	for(int i=0;i<subClasses.size();i++){
		String subClass = subClasses.get(i);
		
		Vector<String> superClasses = RDFProvider.getSuperclasses( subClass);
%>
		superclasses['<%=subClass%>']= [];
		<%
	for(int j=0;j<superClasses.size();j++){
	String superClass = superClasses.get(j);
	if(superClass == null || superClass.length() == 0)
		continue;
%>superclasses['<%=subClass%>'][superclasses['<%=subClass%>'].length] = '<%=superClass%>';
			<%
	}
		subClasses.addAll(RDFProvider.getSubclasses( subClass));
	}
	subClasses = RDFProvider.getSubclasses( className);
	for(int i=0;i<subClasses.size();i++){
		String subClass = subClasses.get(i);
		Vector<String> superClasses = RDFProvider.getSuperclasses( subClass);
%>
		superclasses['<%=subClass%>']= [];
		<%
	for(int j=0;j<superClasses.size();j++){
	String superClass = superClasses.get(j);
	if(superClass == null || superClass.length() == 0)
		continue;
%>superclasses['<%=subClass%>'][superclasses['<%=subClass%>'].length] = '<%=superClass%>';
			<%
	}
		subClasses.addAll(RDFProvider.getSubclasses( subClass));
	}
%>
}