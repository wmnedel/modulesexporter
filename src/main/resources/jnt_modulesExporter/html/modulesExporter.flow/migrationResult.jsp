<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>


<div class="box-1">

    <h2>Custom modules should replace groupID org.jahia.com in pom.xml</h2>

    <table class="table table-striped">
        <thead>
        <tr>
            <th>Module Name</th>
            <th> Version</th>
            <th> Result</th>
        </tr>
        </thead>
        <c:forEach items="${migrationReport}" var="module">
            <tr>
                <td> ${module.bundleName} </td>
                <td> ${module.version} </td>
                <td> ${module.message} </td>
            </tr>
        </c:forEach>
    </table>

    <form:form modelAttribute="environmentInfo" class="form-horizontal" method="post">

        <button id="previous" class="btn btn-primary" type="submit" name="_eventId_previous">
            Back
        </button>
    </form:form>

</div>