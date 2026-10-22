<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>AI Framework</title>
    <style>
        body { font-family: Segoe UI, Arial, sans-serif; max-width: 700px; margin: 40px auto; padding: 0 16px; }
        a { color: #0b5cad; }
    </style>
</head>
<body>
    <h1>AI Framework</h1>
    <p>Plain Spring MVC (no Spring Boot) on Tomcat 11 and Java 21.</p>

    <ul>
        <li><a href="${pageContext.request.contextPath}/ask">Ask the assistant</a> (tools + memory)</li>
        <li><a href="${pageContext.request.contextPath}/workflows">Agentic workflows</a> (sequence, loop, parallel, routing)</li>
        <li><a href="${pageContext.request.contextPath}/health">Health check</a></li>
    </ul>

    <p><small>Server time: ${serverTime}</small></p>
</body>
</html>
