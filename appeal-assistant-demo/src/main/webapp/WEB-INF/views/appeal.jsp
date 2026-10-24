<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Appeal ${claim.id()}</title>
    <%-- while the AI is working, reload the page every 3 seconds --%>
    <c:if test="${appeal.status == 'PREPARING'}"><meta http-equiv="refresh" content="3"></c:if>
    <style>
        body { font-family: Segoe UI, Arial, sans-serif; max-width: 1200px; margin: 24px auto; padding: 0 16px; color: #1d2433; }
        a { color: #1f5fbf; }
        .demo { background: #fff6e8; border-left: 4px solid #b26a00; padding: 6px 12px; font-size: 13px; margin: 8px 0 14px; }
        .head { display: flex; justify-content: space-between; align-items: baseline; border-bottom: 2px solid #dde2ea; padding-bottom: 6px; }
        .status { display: inline-block; padding: 3px 10px; border-radius: 12px; font-weight: 600; font-size: 13px; background: #e8f0fc; color: #1f5fbf; }
        .cols { display: grid; grid-template-columns: 1fr 1.25fr; gap: 18px; margin-top: 14px; }
        .box { border: 1px solid #dde2ea; border-radius: 10px; padding: 12px 14px; margin-bottom: 14px; }
        .box h3 { margin: 0 0 8px; font-size: 14px; text-transform: uppercase; color: #5b6475; letter-spacing: .03em; }
        .pre { white-space: pre-wrap; font-family: Consolas, monospace; font-size: 13px; background: #f5f7fa; padding: 10px; border-radius: 6px; }
        dl { display: grid; grid-template-columns: 130px 1fr; gap: 4px 10px; margin: 0; font-size: 14px; }
        dt { color: #5b6475; font-weight: 600; }
        dd { margin: 0; }
        textarea { width: 100%; height: 360px; font-family: Consolas, monospace; font-size: 13px; padding: 8px; box-sizing: border-box; }
        .warn { background: #fff6e8; border-left: 4px solid #b26a00; padding: 8px 10px; font-size: 13.5px; margin-bottom: 10px; }
        .err { background: #fdecea; border-left: 4px solid #b3261e; padding: 8px 10px; }
        .ok { background: #e9f4ec; border-left: 4px solid #2e7d4f; padding: 8px 10px; }
        .actions { display: flex; gap: 10px; margin-top: 10px; align-items: flex-start; }
        .actions input[type=text] { width: 260px; padding: 6px; }
        button { border: 0; border-radius: 6px; padding: 8px 14px; cursor: pointer; font-size: 14px; color: #fff; }
        .approve { background: #2e7d4f; } .reject { background: #b3261e; } .again { background: #1f5fbf; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; }
        td { padding: 5px 6px; border-bottom: 1px solid #eef0f4; vertical-align: top; }
        details { margin: 4px 0; } summary { cursor: pointer; font-weight: 600; font-size: 13.5px; }
    </style>
</head>
<body>
    <p><a href="${pageContext.request.contextPath}/">&larr; Work list</a></p>
    <div class="head">
        <h1 style="margin:0">Claim ${claim.id()} - <c:out value="${claim.service()}"/></h1>
        <c:if test="${not empty appeal}"><span class="status">${appeal.status.label}</span></c:if>
    </div>
    <div class="demo">Synthetic demo data - invented patient, fictional insurer. Nothing is really sent.</div>

    <div class="cols">
        <!-- LEFT: the facts -->
        <div>
            <div class="box">
                <h3>Denied claim</h3>
                <dl>
                    <dt>Patient</dt><dd><c:out value="${patient.name()}"/> (born ${patient.dateOfBirth()})</dd>
                    <dt>Insurer</dt><dd><c:out value="${claim.insurer()}"/></dd>
                    <dt>Service</dt><dd><c:out value="${claim.service()}"/> &middot; codes ${claim.serviceCodes()} &middot; ${claim.serviceDate()}</dd>
                    <dt>Amount</dt><dd>$${claim.amount()}</dd>
                    <dt>Denial</dt><dd><b>${claim.denialCode()}</b> - <c:out value="${claim.denialRemark()}"/></dd>
                    <dt>Deadline</dt><dd>${claim.appealDeadline()}
                        (<c:choose><c:when test="${claim.daysLeft() lt 0}"><b style="color:#b3261e">deadline passed</b></c:when><c:otherwise>${claim.daysLeft()} days left</c:otherwise></c:choose>)</dd>
                    <c:if test="${not empty appeal.category}"><dt>AI routing</dt><dd>${appeal.category}</dd></c:if>
                </dl>
            </div>

            <c:if test="${not empty appeal.evidence}">
                <div class="box">
                    <h3>Proof collected from the chart</h3>
                    <div class="pre"><c:out value="${appeal.evidence}"/></div>
                </div>
            </c:if>

            <c:if test="${not empty appeal.steps}">
                <div class="box">
                    <h3>AI steps (${appeal.steps.size()})</h3>
                    <c:forEach var="s" items="${appeal.steps}" varStatus="i">
                        <details><summary>${i.count}. ${s.agent()}</summary><div class="pre"><c:out value="${s.output()}"/></div></details>
                    </c:forEach>
                </div>
            </c:if>
        </div>

        <!-- RIGHT: draft + human decision -->
        <div>
            <c:choose>
                <c:when test="${empty appeal}">
                    <div class="box">
                        <p>No AI draft yet.</p>
                        <form method="post" action="${pageContext.request.contextPath}/claims/${claim.id()}/prepare">
                            <button class="again" type="submit">Prepare with AI</button>
                        </form>
                    </div>
                </c:when>

                <c:when test="${appeal.status == 'PREPARING'}">
                    <div class="box"><p><b>The AI is preparing this appeal...</b> This page refreshes by itself (usually 1-2 minutes). Progress appears in the audit log below.</p>
                        <p style="color:#5b6475;font-size:13.5px">Steps: classify the denial &rarr; collect proof with read-only tools &rarr; draft &rarr; check every statement against the source records &rarr; ready for your review.</p></div>
                </c:when>

                <c:when test="${appeal.status == 'FAILED'}">
                    <div class="box err"><b>Failed:</b> <c:out value="${appeal.error}"/></div>
                    <form method="post" action="${pageContext.request.contextPath}/claims/${claim.id()}/prepare">
                        <button class="again" type="submit">Try again</button>
                    </form>
                </c:when>

                <c:when test="${appeal.status.needsReview}">
                    <div class="box">
                        <h3>AI draft - review, edit, then decide</h3>
                        <c:if test="${appeal.status == 'NOT_RECOMMENDED'}">
                            <div class="warn">The AI recommends <b>not</b> appealing. Read the reason; approving accepts this recommendation.</div>
                        </c:if>
                        <c:if test="${appeal.status == 'CORRECTION_SUGGESTED'}">
                            <div class="warn">This is a <b>coding</b> denial, so the AI suggests a corrected claim instead of an appeal letter.</div>
                        </c:if>
                        <c:if test="${not empty appeal.openIssues}">
                            <div class="warn"><b>Checker still found possible unsupported statements:</b><br><c:out value="${appeal.openIssues}"/></div>
                        </c:if>
                        <form method="post" action="${pageContext.request.contextPath}/appeals/${claim.id()}/approve">
                            <textarea name="finalText" required><c:out value="${appeal.draft}"/></textarea>
                            <div class="actions">
                                <button class="approve" type="submit">
                                    <c:choose>
                                        <c:when test="${appeal.status == 'READY_FOR_REVIEW'}">Approve &amp; send appeal (simulated)</c:when>
                                        <c:otherwise>Accept recommendation</c:otherwise>
                                    </c:choose>
                                </button>
                            </div>
                        </form>
                        <form method="post" action="${pageContext.request.contextPath}/appeals/${claim.id()}/reject" class="actions">
                            <input type="text" name="reason" required placeholder="Reason for rejecting">
                            <button class="reject" type="submit">Reject</button>
                        </form>
                    </div>
                </c:when>

                <c:otherwise>
                    <div class="box ${appeal.status == 'APPROVED' ? 'ok' : 'err'}"><b>${appeal.status.label}</b></div>
                    <c:if test="${not empty appeal.finalText}">
                        <div class="box"><h3>Approved text</h3><div class="pre"><c:out value="${appeal.finalText}"/></div></div>
                    </c:if>
                    <c:if test="${appeal.status == 'REJECTED'}">
                        <form method="post" action="${pageContext.request.contextPath}/claims/${claim.id()}/prepare">
                            <button class="again" type="submit">Prepare again with AI</button>
                        </form>
                    </c:if>
                </c:otherwise>
            </c:choose>

            <c:if test="${not empty appeal.audit}">
                <div class="box" style="margin-top:14px">
                    <h3>Audit log</h3>
                    <table>
                        <c:forEach var="ev" items="${appeal.audit}">
                            <tr><td style="white-space:nowrap">${ev.when()}</td><td><b><c:out value="${ev.actor()}"/></b></td><td><c:out value="${ev.action()}"/></td></tr>
                        </c:forEach>
                    </table>
                </div>
            </c:if>
        </div>
    </div>
</body>
</html>
