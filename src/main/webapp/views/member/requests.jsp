<%@page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>

<%@taglib prefix="jstl"	uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@taglib prefix="security" uri="http://www.springframework.org/security/tags"%>
<%@taglib prefix="display" uri="http://displaytag.sf.net"%>

<security:authorize access="hasRole('MEMBER')">		 	

	<form name="filter" id="filter" action="request/member/filter.do" method="post">
		<label for="filter"><spring:message code="request.filter"/></label>
	
		<br/>
	
		<select name="fselect">
  			<option value="ALL">-</option>
  			<option value="PENDING"><spring:message code="request.status.pending"/></option>
 			<option value="APPROVED"><spring:message code="request.status.approved"/></option>
  			<option value="REJECTED"><spring:message code="request.status.rejected"/></option>
		</select>
		
		<input type="submit" name="refresh" id="refresh" value="<spring:message code ="request.filter.button"/>"/>
	
	</form>
	
	<br/>

	<display:table pagesize="5" name="requests" id="row" class="displaytag" 
					requestURI="request/member/list.do">
					
		<jstl:choose>
			<jstl:when test="${row.status.toString()=='APPROVED'}">
				<jstl:set var="color" value="green" />
			</jstl:when>
			
			<jstl:when test="${row.status.toString()=='REJECTED'}">
				<jstl:set var="color" value="orange" />
			</jstl:when>
				
			<jstl:when test="${row.status.toString()=='PENDING'}">
				<jstl:set var="color" value="grey" />
			</jstl:when>
				
			<jstl:otherwise>
				<jstl:set var="color" value="black" />
			</jstl:otherwise>
		</jstl:choose>
		
		<display:column titleKey="request.procession">
			<jstl:out value="${row.procession.title}"/>
		</display:column>
					
		<display:column property="status" titleKey="request.status" style="color:${color}"/>
		
		<display:column property="rowNumber" titleKey="request.rowNumber" style="color:${color}"/>
		
		<display:column property="columnNumber" titleKey="request.columnNumber" style="color:${color}"/>
			
		<display:column property="reasonDescription" titleKey="request.reasonDescription" style="color:${color}"/>
		
		<display:column titleKey="action">
			<jstl:if test="${row.status.toString()=='PENDING'}">
				<spring:url var="deleteRequest" value="/request/member/delete.do">
					<spring:param name="requestId" value="${row.id}" />
				</spring:url>
				<a href="${deleteRequest}" onclick="return confirm('<spring:message code="request.delete.confirmation" />')">
					<spring:message code="request.delete" />				
				</a>
			</jstl:if>
			<jstl:if test="${row.status.toString()!='PENDING'}">
				<spring:message code="no.action"/>
			</jstl:if>
		</display:column>
				
	</display:table>
	
	<jstl:if test="${flag==false}"> 
		<p style="color:red"><spring:message code="request.delete.error"/></p>
	</jstl:if>
	<jstl:if test="${flag==true}">
		<p style="color:red"><spring:message code="request.delete.ok"/></p>
	</jstl:if>
	
</security:authorize>