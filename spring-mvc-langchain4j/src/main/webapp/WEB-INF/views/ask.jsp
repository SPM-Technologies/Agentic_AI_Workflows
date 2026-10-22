<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Ask the assistant</title>
    <style>
        body { font-family: Segoe UI, Arial, sans-serif; max-width: 700px; margin: 40px auto; padding: 0 16px; }
        textarea { width: 100%; height: 80px; }
        .question { background: #eef2e6; border-left: 4px solid #5a7d1e; padding: 10px; margin-top: 20px; white-space: pre-wrap; }
        .answer { background: #f2f6fb; border-left: 4px solid #0b5cad; padding: 12px; margin-top: 6px; white-space: pre-wrap; }
    </style>
</head>
<body>
    <p><a href="${pageContext.request.contextPath}/">&larr; Home</a></p>
    <h1>Ask the assistant</h1>
    <p><small>Tools: current date/time, order status (orders 101-103). Remembers this conversation.</small></p>

    <%-- c:out escapes HTML, so typed text can't inject tags into the page --%>
    <c:forEach var="turn" items="${history}">
        <div class="question"><b>You:</b> <c:out value="${turn.question}"/></div>
        <div class="answer"><c:out value="${turn.answer}"/></div>
    </c:forEach>

    <form method="post" action="${pageContext.request.contextPath}/ask">
        <p><textarea name="question" required placeholder="Type your question..."></textarea></p>
        <button type="submit">Ask</button>
    </form>

    <c:if test="${not empty history}">
        <form method="post" action="${pageContext.request.contextPath}/ask/clear">
            <p><button type="submit">New chat (forget conversation)</button></p>
        </form>
    </c:if>
</body>
</html>
