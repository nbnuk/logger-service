<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<meta name="layout" content="${grailsApplication.config.skin.layout}" />
	<meta name="breadcrumb" content="Logger Service" />
	<title>Logger service | ${grailsApplication.config.skin.orgNameLong}</title>
	<style type="text/css">
		/* Styles adapted from logger-web-services.html */
		.content {
			padding: 20px;
			/* Removed text-align: center; */
		}
		.content h1 {
			font-size: 24px;
			color: #333;
		}
		.content p.lead { /* Applied lead style from original */
			font-size: 16px;
			color: #666;
			margin-bottom: 20px;
		}
		.scrollable-panel {
			background-color: #f0f4f8; /* Pleasing light blue-gray background */
			width: 95%; /* Use 95% of available width */
			margin: 20px auto; /* Center panel with some top/bottom margin */
			padding: 20px;
			border-radius: 15px;
			box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
			max-height: 600px; /* Increased height for potentially more reports */
			overflow-y: auto; /* Enable vertical scrolling */
			overflow-x: hidden; /* Prevent horizontal scroll */
		}
		.services-grid {
			display: grid;
			grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
			gap: 20px;
		}
		.service-card {
			background-color: #f9fafb; /* Very light tint for contrast */
			border: 1px solid #ddd;
			border-radius: 25px; /* Lozenge shape */
			padding: 20px;
			text-align: left;
			box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
			transition: transform 0.2s;
		}
		.service-card:hover {
			 transform: translateY(-3px); /* Lift effect on hover */
		}
		.service-card h3 {
			font-size: 18px;
			margin-top: 0; /* Remove default margin */
			margin-bottom: 10px;
			/* Removed flex alignment for title, handled differently */
		}
		 .service-card h3 a {
			color: #003087; /* Link color from nav */
			text-decoration: none;
		 }
		 .service-card h3 a:hover {
			text-decoration: underline;
		 }
		.service-card p {
			font-size: 14px;
			color: #666;
			margin-bottom: 15px;
		}
		.service-card .controls {
			display: flex;
			align-items: center;
			gap: 10px;
		}
		.service-card select {
			padding: 8px 12px; /* Increased padding */
			border: 1px solid #ccc; /* Slightly softer border */
			border-radius: 20px; /* Rounded corners for select */
			background-color: #fff;
			font-size: 14px;
			flex-grow: 1; /* Allow select boxes to grow */
			min-width: 100px; /* Minimum width */
		}
		.service-card .run-btn {
			background-color: #e0e0e0;
			color: #000;
			border: none;
			border-radius: 25px; /* Match card rounding */
			padding: 8px 16px;
			cursor: pointer;
			font-size: 14px;
			font-weight: bold; /* Make text bold */
			box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
			transition: background-color 0.2s, transform 0.1s;
			display: flex;
			align-items: center;
			gap: 5px;
			white-space: nowrap; /* Prevent button text wrapping */
		}
		.service-card .run-btn:hover {
			background-color: #d0d0d0;
		}
		.service-card .run-btn:active {
			transform: scale(0.95);
		}
		/* Removed original file-type-icon styles */
		/* Removed original li style */

		/* Style for header container */
		.header-container {
			display: flex; /* Use flexbox */
			align-items: baseline; /* Align based on text baseline */
			gap: 15px; /* Add some space between the elements */
			flex-wrap: wrap; /* Allow wrapping on very small screens */
			margin-bottom: 20px; /* Add margin below the header line */
		}
		.header-container h1,
		.header-container p.lead {
			margin-bottom: 0; /* Remove default bottom margin for elements inside */
		}

	</style>
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
