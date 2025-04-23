<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<meta name="layout" content="${grailsApplication.config.skin.layout}" />
	<meta name="breadcrumb" content="Logger Service" />
	<title>Logger service | ${grailsApplication.config.skin.orgNameLong}</title>
	<asset:stylesheet src="logger-service.css"/>
</head>
<body>
<div class="container content">
	<div class="header-container"> <!-- Wrap H1 and P -->
		<h1>Logger web services</h1>
		<p class="lead">
			Below is a list of reporting services that return JSON or CSV.
		</p>
	</div>
	<div class="scrollable-panel">
		<div class="services-grid">
			<div class="service-card">
				<form action="${request.contextPath}/service/logger/reasons" method="GET">
					<h3><strong>User reasons</strong> codes</h3>
					<p>View the JSON file containing codes for user reasons, useful for understanding motivations behind data usage.</p>
					<div class="controls">
						<button type="submit" class="run-btn" title="Run Report"><i class="fas fa-play"></i></button>
					</div>
				</form>
			</div>

			<div class="service-card">
				<form action="${request.contextPath}/service/logger/sources" method="GET">
					<h3><strong>Sources</strong> codes</h3>
					<p>View the JSON file listing source codes.</p>
					<div class="controls">
						<button type="submit" class="run-btn" title="Run Report"><i class="fas fa-play"></i></button>
					</div>
				</form>
			</div>

			<div class="service-card">
				<form action="${request.contextPath}/service/logger/events" method="GET">
					<h3><strong>Events</strong> codes</h3>
					<p>View the JSON file listing event codes, which categorize different types of logged activities.</p>
					<div class="controls">
						<button type="submit" class="run-btn" title="Run Report"><i class="fas fa-play"></i></button>
					</div>
				</form>
			</div>

			<div class="service-card">
				 <form action="${request.contextPath}/service/reasonBreakdown" method="GET">
					<input type="hidden" name="eventId" value="1002" />
					<input type="hidden" name="entityUid" value="in4" />
					<h3><strong>Reason</strong> breakdown</h3>
					<p>Reason breakdown (last month, 3 month, 1 year, all, example for downloads from Australian Museum).</p>
					 <div class="controls">
						 <select name="date-range">
							<option>Last Month</option>
							<option>Last 3 Months</option>
							<option>Last 1 Year</option>
							<option>All Time</option>
						</select>
						<select name="format">
							<option selected>JSON</option>
							<option>CSV</option>
						</select>
						<button type="submit" class="run-btn" title="Run Report"><i class="fas fa-play"></i></button>
					</div>
				 </form>
			</div>

			<div class="service-card">
				<form action="${request.contextPath}/service/sourceBreakdown" method="GET">
					<input type="hidden" name="eventId" value="1002" />
					<input type="hidden" name="entityUid" value="in4" />
					<h3><strong>Source</strong> breakdown</h3>
					<p>Source breakdown (last month, 3 month, 1 year, all, example for downloads from Australian Museum).</p>
					 <div class="controls">
						 <select name="date-range">
							<option>Last Month</option>
							<option>Last 3 Months</option>
							<option>Last 1 Year</option>
							<option>All Time</option>
						</select>
						<select name="format">
							<option selected>JSON</option>
							<option>CSV</option>
						</select>
						<button type="submit" class="run-btn" title="Run Report"><i class="fas fa-play"></i></button>
					</div>
				</form>
			</div>

			 <div class="service-card">
				<form action="${request.contextPath}/service/reasonBreakdownMonthly" method="GET">
					<input type="hidden" name="eventId" value="1002" />
					<input type="hidden" name="sourceId" value="2001" />
					<input type="hidden" name="entityUid" value="in4" />
					<h3><strong>Reason Monthly</strong> breakdown</h3>
					<p>Reason Monthly breakdown (event and record counts only) with optional reasonId and sourceId filters (example for downloads from source ALA4R).</p>
					 <div class="controls">
						 <select name="date-range">
							<option>Last Month</option>
							<option>Last 3 Months</option>
							<option>Last 1 Year</option>
							<option>All Time</option>
						</select>
						<select name="format">
							<option selected>JSON</option>
							<option>CSV</option>
						</select>
						<button type="submit" class="run-btn" title="Run Report"><i class="fas fa-play"></i></button>
					</div>
				</form>
			</div>

			<div class="service-card">
				<form action="${request.contextPath}/service/reasonBreakdown" method="GET"> <%-- Corrected action path --%>
					<input type="hidden" name="eventId" value="1002" />
					<input type="hidden" name="entityUid" value="in4" />
					<h3><strong>Reason</strong> breakdown by month</h3>
					<p>Download CSV file for Reason breakdown by month (all records, example for Australian Museum).</p>
					 <div class="controls">
						 <select name="date-range">
							<option>Last Month</option>
							<option>Last 3 Months</option>
							<option>Last 1 Year</option>
							<option>All Time</option>
						</select>
						<select name="format">
							<option>JSON</option>
							<option selected>CSV</option>
						</select>
						<button type="submit" class="run-btn" title="Run Report"><i class="fas fa-play"></i></button>
					</div>
				</form>
			</div>

			<div class="service-card">
				<form action="${request.contextPath}/service/emailBreakdown" method="GET"> <%-- Corrected action path --%>
					<input type="hidden" name="eventId" value="1002" />
					<input type="hidden" name="entityUid" value="in4" />
					<h3><strong>User category</strong> breakdown by month</h3>
					<p>Download CSV file for User category breakdown by month (all records, example for downloads from Australian Museum).</p>
					 <div class="controls">
						 <select name="date-range">
							<option>Last Month</option>
							<option>Last 3 Months</option>
							<option>Last 1 Year</option>
							<option>All Time</option>
						</select>
						 <select name="format">
							<option>JSON</option>
							<option selected>CSV</option>
						</select>
						<button type="submit" class="run-btn" title="Run Report"><i class="fas fa-play"></i></button>
					</div>
				</form>
			</div>

			 <div class="service-card">
				<form action="${request.contextPath}/service/sourceBreakdown" method="GET"> <%-- Corrected action path --%>
					<input type="hidden" name="eventId" value="1002" />
					<input type="hidden" name="entityUid" value="in4" />
					<h3><strong>Source and reason</strong> breakdown by month</h3>
					<p>Download CSV file for Source and reason breakdown by month (all records, example for downloads from Australian Museum).</p>
					 <div class="controls">
						 <select name="date-range">
							<option>Last Month</option>
							<option>Last 3 Months</option>
							<option>Last 1 Year</option>
							<option>All Time</option>
						</select>
						<select name="format">
							<option>JSON</option>
							<option selected>CSV</option>
						</select>
						<button type="submit" class="run-btn" title="Run Report"><i class="fas fa-play"></i></button>
					</div>
				</form>
			</div>

		</div>
	</div>
</div>
</body>
</html>
