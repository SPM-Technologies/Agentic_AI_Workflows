<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Agentic workflows</title>
    <style>
        body { font-family: Segoe UI, Arial, sans-serif; max-width: 800px; margin: 40px auto; padding: 0 16px; }
        textarea { width: 100%; height: 120px; font-family: Consolas, monospace; }
        label { display: block; margin: 4px 0; }
        .hint { color: #555; font-size: 0.9em; }
        .output { background: #f2f6fb; border-left: 4px solid #0b5cad; padding: 12px; white-space: pre-wrap; font-family: Consolas, monospace; }
        details { margin: 6px 0; } summary { cursor: pointer; font-weight: bold; }
        .step { background: #f7f7f7; padding: 10px; white-space: pre-wrap; font-family: Consolas, monospace; }
    </style>
</head>
<body>
    <p><a href="${pageContext.request.contextPath}/">&larr; Home</a></p>
    <h1>Agentic workflows</h1>

    <form method="post" action="${pageContext.request.contextPath}/workflows">
        <label><input type="radio" name="pattern" value="SEQUENCE" ${pattern == 'SEQUENCE' ? 'checked' : ''}>
            <b>6.3 Sequence</b> - writer, then reviewer <span class="hint">(input: a coding task)</span></label>
        <label><input type="radio" name="pattern" value="LOOP" ${pattern == 'LOOP' ? 'checked' : ''}>
            <b>6.4 Loop</b> - write, then review / score / fix until score &ge; 0.8 (max 3 rounds) <span class="hint">(input: a coding task)</span></label>
        <label><input type="radio" name="pattern" value="PARALLEL" ${pattern == 'PARALLEL' ? 'checked' : ''}>
            <b>6.5a Parallel</b> - security, performance and readability reviews at once <span class="hint">(input: Java code)</span></label>
        <label><input type="radio" name="pattern" value="ROUTING" ${pattern == 'ROUTING' ? 'checked' : ''}>
            <b>6.5b Routing</b> - classify, then send to the code, order or general expert <span class="hint">(input: any question)</span></label>

        <p><textarea name="input" required placeholder="e.g. Write a method that checks if a string is a palindrome"><c:out value="${input}"/></textarea></p>
        <button type="submit">Run</button> <span class="hint">Takes a few seconds to a minute - several Claude calls.</span>
    </form>

    <c:if test="${not empty result}">
        <h2>Result</h2>
        <div class="output"><c:out value="${result.output}"/></div>

        <h2>Steps (${result.steps.size()} agent calls)</h2>
        <c:forEach var="step" items="${result.steps}" varStatus="s">
            <details>
                <summary>${s.count}. <c:out value="${step.agent}"/></summary>
                <div class="step"><c:out value="${step.output}"/></div>
            </details>
        </c:forEach>
    </c:if>
</body>
</html>
