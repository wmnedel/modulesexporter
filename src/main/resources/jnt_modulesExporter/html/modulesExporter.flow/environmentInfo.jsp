<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="javascript" resources="jquery.min.js,admin-bootstrap.js,spinner.js"/>
<template:addResources type="css" resources="style.css"/>


<c:set var="actionUrl" value="${url.base}${currentNode.path}.exportModules.do"/>


<div class="box-1">


    <form:form id="form1" modelAttribute="environmentInfo" method="post">
        <%@ include file="validation.jspf" %>
        <h1><fmt:message
                key="modules-exporter.title"></fmt:message></h1>
        <p>
            <fmt:message
                    key="module.desc"></fmt:message>
        </p>
        <p>
            <fmt:message
                    key="module.desc2"></fmt:message>
        </p>
        <h2> <fmt:message
                key="module.header"></fmt:message> </h2>


        <fieldset>

            <div class="container-fluid">
                <div class="row-fluid">
                    <label for="remoteScheme"><fmt:message
                            key="lbl.remoteHost"></fmt:message> </label>

                    <select class="span1" id="remoteScheme" name="remoteScheme" path="remoteScheme"
                            value="${environmentInfo.remoteScheme}">
                        <option value="https">https</option>
                        <option value="http">http</option>
                    </select>

                    <input class="span4" path="remoteHost" type="text" name="remoteHost" id="remoteHost"
                           value="${environmentInfo.remoteHost}" required/>

                    <input class="span1" path="remotePort" type="text" name="remotePort" id="remotePort"
                           value="${environmentInfo.remotePort}" required/>
                </div>

            </div>

            <div class="container-fluid">
                <div class="row-fluid">
                    <div class="span4">
                        <label for="remoteToolsUser"><fmt:message
                                key="lbl.remoteToolsUser"></fmt:message> </label>
                        <input class="form-control" path="remoteToolsUser" type="text" name="remoteToolsUser"
                               id="remoteToolsUser" value="${environmentInfo.remoteToolsUser}" required/>

                    </div>
                </div>
            </div>

            <div class="container-fluid">
                <div class="row-fluid">
                    <div class="span4">
                        <label for="remoteToolsPwd"><fmt:message
                                key="lbl.remoteToolsPwd"></fmt:message> </label>
                        <input class="form-control" path="remoteToolsPwd" type="password" name="remoteToolsPwd"
                               id="remoteToolsPwd" value="${environmentInfo.remoteToolsPwd}" required/>
                    </div>
                </div>
            </div>


            <div class="container-fluid">
                <div class="row-fluid">
                    <div class="span4">
                        <form:checkbox class="form-control" path="autoStart" name="autoStart"
                               id="autoStart" value="${environmentInfo.autoStart}"/>
                       <span> <fmt:message
                                key="lbl.autoStart"></fmt:message></span>
                    </div>
                </div>
            </div>


            <div class="container-fluid">
                <div class="row-fluid">
                    <div class="span4">
                        <form:checkbox class="form-control" path="srcStartedOnly" name="srcStartedOnly"
                                       id="srcStartedOnly" value="${environmentInfo.srcStartedOnly}"/>
                        <span> <fmt:message
                                key="lbl.srcStartedOnly"></fmt:message></span>
                    </div>
                </div>
            </div>


            <div class="container-fluid">
                <div class="row-fluid">
                    <div class="span4">
                        <div class="form-group">
                            <button id="migrateModules" class="btn btn-primary" type="submit"
                                    name="_eventId_migrateModules">
                                <fmt:message key="lbl.btnSubmit"></fmt:message>
                            </button>
                        </div>
                    </div>

                </div>
            </div>

        </fieldset>

    </form:form>
    <div class="loading-spinner">
        <span> Processing, please wait...</span>
    </div>


</div>

<c:set var="redirectUrl" value="${renderContext.mainResource.node.path}.html" scope="session" />

<form:form id="form2" method="post" action="${actionUrl}">
    <div class="box-1">
        <div class="container-fluid">
            <div class="row-fluid">
                <div class="span5">
                    <h3><strong> You may also export and download all active modules from the local instance </strong></h3>
                    <button id="downloadModules2" class="btn btn-success" type="submit">
                        <fmt:message key="lbl.btnDownload"> </fmt:message>
                    </button>
                </div>
            </div>
        </div>
    </div>
</form:form>

