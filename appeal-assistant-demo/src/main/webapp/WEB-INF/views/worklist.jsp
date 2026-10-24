<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Denied claims - work list</title>
    <style>
        body { font-family: Segoe UI, Arial, sans-serif; max-width: 1100px; margin: 30px auto; padding: 0 16px; color: #1d2433; }
        .demo { background: #fff6e8; border-left: 4px solid #b26a00; padding: 8px 12px; margin-bottom: 16px; font-size: 14px; }
        table { width: 100%; border-collapse: collapse; }
        th, td { text-align: left; padding: 9px 8px; border-bottom: 1px solid #dde2ea; vertical-align: top; font-size: 14px; }
        th { background: #f5f7fa; font-size: 12px; text-transform: uppercase; color: #5b6475; }
        .code { font-weight: 600; }
        .remark { color: #5b6475; font-size: 12.5px; }
        .status { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 12px; font-weight: 600; border: 1px solid #5b6475; color: #5b6475; }
        .s-review { border-color: #1f5fbf; color: #1f5fbf; } .s-done { border-color: #2e7d4f; color: #2e7d4f; } .s-bad { border-color: #b3261e; color: #b3261e; }
        button, .btn { background: #1f5fbf; color: #fff; border: 0; border-radius: 6px; padding: 6px 12px; cursor: pointer; font-size: 13px; text-decoration: none; display: inline-block; }
        .btn-light { background: #e8f0fc; color: #1f5fbf; }
    </style>
</head>
<body>
    <h1>Denied claims - work list</h1>
    <div class="demo"><b>Demo with synthetic data</b> - invented patients and fictional insurers. AI prepares each appeal; a biller reviews and approves. Nothing is really sent.</div>

    <table>
        <tr><th>Claim</th><th>Patient</th><th>Service</th><th>Amount</th><th>Denial</th><th>Appeal deadline</th><th>Status</th><th></th></tr>
        <c:forEach var="cl" items="${claims}">
            <c:set var="ap" value="${appeals[cl.id()]}"/>
            <tr>
                <td>${cl.id()}<br><span class="remark"><c:out value="${cl.insurer()}"/></span></td>
                <td><c:out value="${data.patient(cl.patientId()).name()}"/></td>
                <td><c:out value="${cl.service()}"/><br><span class="remark">codes ${cl.serviceCodes()} &middot; ${cl.serviceDate()}</span></td>
                <td>$${cl.amount()}</td>
                <td><span class="code">${cl.denialCode()}</span><br><span class="remark"><c:out value="${cl.denialRemark()}"/></span></td>
                <td>${cl.appealDeadline()}<br><span class="remark"><c:choose><c:when test="${cl.daysLeft() lt 0}"><b style="color:#b3261e">deadline passed</b></c:when><c:otherwise>${cl.daysLeft()} days left</c:otherwise></c:choose></span></td>
                <td>
                    <c:choose>
                        <c:when test="${empty ap}"><span class="status">New</span></c:when>
                        <c:when test="${ap.status.needsReview}"><span class="status s-review">${ap.status.label}</span></c:when>
                        <c:when test="${ap.status == 'APPROVED'}"><span class="status s-done">${ap.status.label}</span></c:when>
                        <c:when test="${ap.status == 'FAILED' or ap.status == 'REJECTED'}"><span class="status s-bad">${ap.status.label}</span></c:when>
                        <c:otherwise><span class="status">${ap.status.label}</span></c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${empty ap}">
                            <form method="post" action="${pageContext.request.contextPath}/claims/${cl.id()}/prepare">
                                <button type="submit">Prepare with AI</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <a class="btn btn-light" href="${pageContext.request.contextPath}/appeals/${cl.id()}">Open</a>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
    </table>
</body>
</html>
