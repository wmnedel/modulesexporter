<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>


<div class="box-1">

    <h2>Report</h2>

    <table class="table table-striped">
        <thead>
        <tr>
            <th>Name</th>
            <th>Version Id</th>
            <th>Group ID</th>
            <th>jmix:cmContentTreeDisplayable</th>
            <th>serverSettings</th>
            <th>siteSettings</th>
            <th>Spring</th>
        </tr>
        </thead>
        <c:forEach items="${migrationReport}" var="module">
            <tr>
                <td>${module.moduleName}</td>
                <td>${module.moduleVersion}</td>
                <td>${module.moduleGroupId}</td>
                <td>${module.nodeTypes}</td>
                <td>${module.serverSettings}</td>
                <td>${module.siteSettings}</td>
                <td>${module.hasSpringBean}</td>
            </tr>
        </c:forEach>
    </table>

    <form:form modelAttribute="environmentInfo" class="form-horizontal" method="post">

        <button id="previous" class="btn btn-primary" type="submit" name="_eventId_previous">
            Back
        </button>
    </form:form>

    <c:set var="redirectUrl" value="${renderContext.mainResource.node.path}.html" scope="session" />

</div>