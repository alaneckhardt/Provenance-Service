<%@ page language="java" import="provenanceService.*,com.hp.hpl.jena.ontology.*,java.util.Iterator,java.util.*,java.net.*,java.text.SimpleDateFormat,java.util.ArrayList,java.io.*,java.net.*,java.util.Vector" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<% 
  ParameterHelper.setSess(session); 
	ParameterHelper.setReq(request);

	String includeFirst = (String)ParameterHelper.getParameter("includeFirst",  "");
	String liClass = (String)ParameterHelper.getParameter("liClass",  "");
	String liStyle = (String)ParameterHelper.getParameter("liStyle",  "");
	String ulClass = (String)ParameterHelper.getParameter("ulClass",  "");
	String ulStyle = (String)ParameterHelper.getParameter("ulStyle",  "");
	String ulId = (String)ParameterHelper.getParameter("ulId",  "");
	String onClick = (String)ParameterHelper.getParameter("onClick",  "");
	String innerUlStyle = (String)ParameterHelper.getParameter("innerUlStyle",  "padding-left:15px");
	String ontologies = (String)ParameterHelper.getParameter("ontologies",  "general");
	String[] labels = ontologies.split("#");
	String className = (String)ParameterHelper.getParameter("className",  "http://openprovenance.org/ontology#Process");
	
	Vector<String> subClasses = new Vector<String>();
	subClasses.addAll(RDFProvider.getSubclasses(className));
%>
<%!	//Function for recursive loading the ontology tree.
    // There is a lot of parameters, but unfortunatelly, the method doesn't see the jsp variables.
 public String loadTree(String content, String className, 
		 String liClass, String liStyle, String onClick, String ulClass, String ulStyle, String innerUlStyle, String[] labels){

	Vector<String> subClasses = new Vector<String>();
	for(int i=0;i<labels.length;i++){
		subClasses.addAll(RDFProvider.getSubclasses( className));
	}
	String classNameFull = className;
	className=className.substring(className.indexOf('#')+1);
	//Name of class
	/*if(subClasses.size() == 0){
		content += "<li style=\""+liStyle+"\" class=\""+liClass+"\" rel=\"resource\"><span style=\"float:left; margin-right:5px;\" class=\"ui-icon ui-icon-info\"></span><a href=\"#\" style=\"float:left;\" >" + className+"</a>";
	}
	else{ // start a new ul at the end of string.
		content += "<li style=\""+liStyle+"\" class=\""+liClass+" expandable\" rel=\"resource\"><span style=\"float:left; margin-right:5px;\" class=\"ui-icon ui-icon-info\"></span><a href=\"#\" style=\"float:left;\" >" + className + "</a><ul  style=\""+ulStyle+"\" class=\""+ulClass+"\">";		
	}*/
	content += "<li style=\""+liStyle+"\" class=\""+liClass+"\" data-class=\""+ classNameFull+"\" rel=\"resource\"><a href=\"#\" onClick=\""+onClick.replaceAll("#className", className)+"\" >" + className+"</a><br/>";
	
	if(subClasses.size() > 0){
		content += "<ul style=\""+innerUlStyle+"\">";
		//Adding all children
		for(String subClass : subClasses){
			content += loadTree("", subClass, liClass, liStyle, onClick, ulClass, ulStyle, innerUlStyle, labels);
		}
		// closing ul and li
		content += "</ul>";
	}
	content += "</li>";
	
	return content;
}
%>
		<ul id="<%=ulId%>" style="<%=ulStyle%>">
		<% 
		if("true".equals(includeFirst)){
			%><li style="<%=liStyle%>" class="<%=liClass%>" data-class="<%=className%>" rel="resource"><a href="#" onClick="<%=onClick.replaceAll("#className", className.substring(className.indexOf('#')+1))%>" ><%=className.substring(className.indexOf('#')+1) %></a><br/></li>
			<%
		}
				for(String subClass : subClasses){
					String content = loadTree("", subClass,  liClass, liStyle, onClick, ulClass, ulStyle, innerUlStyle,labels);%>
					<%=content %>
			<%} %>
		</ul>	