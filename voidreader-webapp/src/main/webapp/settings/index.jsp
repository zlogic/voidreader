<%@ page session="false" trimDirectiveWhitespaces="true" pageEncoding="utf-8"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<jsp:useBean id="settings" class="org.zlogic.voidreader.web.SettingsRequestBean" scope="page">
	<jsp:setProperty name="settings" property="request" value="${pageContext.request}" />
</jsp:useBean>
<!DOCTYPE html>
<html>
	<head>
		<title>Void Reader Settings</title>
	</head>
	<body>
		<h2>Void Reader Settings for ${pageContext.request.userPrincipal.name}</h2>
		<form action="../admin/settings" method="post" accept-charset="utf-8">
			<p>
				Email to: <input type="email" name="email.to" value="${settings.settings.mailTo}">
			</p>
			<p>
				Cache expire days: <input type="number" name="cache.expire_days" value="${settings.settings.cacheExpireDays}">
			</p>
			<p>
				PDF enable: <input type="checkbox" name="pdf.enable" ${settings.settings.enablePdf?"checked":""}>
			</p>
			<p>
				Feed connect timeout milliseconds: <input type="number" name="feed.connect_timeout" value="${settings.settings.feedConnectTimeout}">
			</p>
			<p>
				Feed read timeout milliseconds: <input type="number" name="feed.read_timeout" value="${settings.settings.feedReadTimeout}">
			</p>
			<p>
				OPML: <br><textarea name="opml">${fn:escapeXml(settings.settings.opml)}</textarea>
			</p>
			<button type="submit">Submit</button>
		</form>
	</body>
</html>
