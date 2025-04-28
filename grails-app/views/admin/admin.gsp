<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="layout" content="${grailsApplication.config.skin.layout}" />
  <meta name="breadcrumb" content="Logger Admin" />
  <title>Logger Service Administration | ${grailsApplication.config.skin.orgNameLong}</title>
  <asset:stylesheet src="logger-service.css"/>
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
<div class="container content">
  <div class="header-container">
    <h1>Logger Service Administration</h1>
    <p class="lead">
      Manage and view logger service data and web services.
    </p>
  </div>

  <div class="scrollable-panel">
    <h2>All web services</h2>
    <p>
      Below is a list of all web services exposed by the logger service.
    </p>

    <div class="services-grid">
      <div class="service-card">
        <h3><strong>Reasons</strong> codes</h3>
        <p>View the JSON file containing codes for user reasons, useful for understanding motivations behind data usage.</p>
        <div class="controls">
          <a href="${request.contextPath}/service/logger/reasons" class="run-btn" title="View Service"><i class="fas fa-external-link-alt"></i> Open</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Events</strong> codes</h3>
        <p>View the JSON file listing event codes, which categorize different types of logged activities.</p>
        <div class="controls">
          <a href="${request.contextPath}/service/logger/events" class="run-btn" title="View Service"><i class="fas fa-external-link-alt"></i> Open</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Sources</strong> codes</h3>
        <p>View the JSON file listing source codes identifying where activities originated from.</p>
        <div class="controls">
          <a href="${request.contextPath}/service/logger/sources" class="run-btn" title="View Service"><i class="fas fa-external-link-alt"></i> Open</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Event</strong> details (example)</h3>
        <p>Example of a specific log event (ID: 1) with complete details.</p>
        <div class="controls">
          <a href="${request.contextPath}/service/logger/1" class="run-btn" title="View Service"><i class="fas fa-external-link-alt"></i> Open</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Monthly</strong> breakdown</h3>
        <p>Monthly breakdown of log events for a specific entity (example: dr143) and event type (ID: 1) in 2021.</p>
        <div class="controls">
          <a href="${request.contextPath}/service/logger/get.json?q=dr143&eventTypeId=1&year=2021" class="run-btn" title="View Service"><i class="fas fa-external-link-alt"></i> Open</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Reason</strong> breakdown (CSV)</h3>
        <p>Download CSV with reason breakdown for a specific entity (dr143) and event type (ID: 1).</p>
        <div class="controls">
          <a href="${request.contextPath}/service/reasonBreakdown?entityUid=dr143&eventId=1&format=CSV" class="run-btn" title="View Service"><i class="fas fa-download"></i> Download</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Reason</strong> breakdown (JSON)</h3>
        <p>View reason breakdown in JSON format for a specific entity (dr143) and event type (ID: 1).</p>
        <div class="controls">
          <a href="${request.contextPath}/service/reasonBreakdown?entityUid=dr143&eventId=1" class="run-btn" title="View Service"><i class="fas fa-external-link-alt"></i> Open</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Reason</strong> monthly breakdown</h3>
        <p>Monthly breakdown by reason for a specific entity (dr143), event type (ID: 1), and reason (ID: 1).</p>
        <div class="controls">
          <a href="${request.contextPath}/service/reasonBreakdownMonthly?entityUid=dr143&eventId=1&reasonId=1" class="run-btn" title="View Service"><i class="fas fa-external-link-alt"></i> Open</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Email</strong> breakdown (CSV)</h3>
        <p>Download CSV with email breakdown for a specific entity (dr143) and event type (ID: 1).</p>
        <div class="controls">
          <a href="${request.contextPath}/service/emailBreakdown?entityUid=dr143&eventId=1&format=CSV" class="run-btn" title="View Service"><i class="fas fa-download"></i> Download</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Email</strong> breakdown (JSON)</h3>
        <p>View email breakdown in JSON format for a specific entity (dr143) and event type (ID: 1).</p>
        <div class="controls">
          <a href="${request.contextPath}/service/emailBreakdown?entityUid=dr143&eventId=1" class="run-btn" title="View Service"><i class="fas fa-external-link-alt"></i> Open</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>POST</strong> new log event</h3>
        <p>Endpoint to upload a new (JSON) log event via HTTP POST request.</p>
        <div class="controls">
          <div class="run-btn" title="POST endpoint" style="cursor: default;"><i class="fas fa-info-circle"></i> ${request.contextPath}/service/logger</div>
        </div>
      </div>
    </div>
  </div>

  <div class="scrollable-panel">
    <h2>User reports</h2>
    <div class="services-grid">
      <div class="service-card">
        <h3><strong>User</strong> reports</h3>
        <p>Download a detailed user report for a specific set of entities.</p>
        <div class="controls">
          <a href="${request.contextPath}/admin/userReport" class="run-btn" title="View Report"><i class="fas fa-file-alt"></i> Generate Report</a>
        </div>
      </div>
    </div>
  </div>

  <div class="scrollable-panel">
    <h2>Data view</h2>
    <p>
      Below is a list of data views for each table in the logger service database.
    </p>
    <div class="services-grid">
      <div class="service-card">
        <h3><strong>Log</strong> Events</h3>
        <p>View all log events stored in the database.</p>
        <div class="controls">
          <a href="${request.contextPath}/admin/logEvent" class="run-btn" title="View Data"><i class="fas fa-table"></i> View</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Log</strong> Details</h3>
        <p>View detailed information for each log event.</p>
        <div class="controls">
          <a href="${request.contextPath}/admin/logDetail" class="run-btn" title="View Data"><i class="fas fa-table"></i> View</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Event</strong> Types</h3>
        <p>View or manage event type classifications.</p>
        <div class="controls">
          <a href="${request.contextPath}/admin/logEventType" class="run-btn" title="View Data"><i class="fas fa-table"></i> View</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Reason</strong> Types</h3>
        <p>View or manage reason type classifications.</p>
        <div class="controls">
          <a href="${request.contextPath}/admin/logReasonType" class="run-btn" title="View Data"><i class="fas fa-table"></i> View</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Source</strong> Types</h3>
        <p>View or manage source type classifications.</p>
        <div class="controls">
          <a href="${request.contextPath}/admin/logSourceType" class="run-btn" title="View Data"><i class="fas fa-table"></i> View</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Remote</strong> Addresses</h3>
        <p>View IP address information for log events.</p>
        <div class="controls">
          <a href="${request.contextPath}/admin/remoteAddress" class="run-btn" title="View Data"><i class="fas fa-table"></i> View</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Event Summary</strong> Totals</h3>
        <p>View aggregated totals across all event types.</p>
        <div class="controls">
          <a href="${request.contextPath}/admin/eventSummaryTotal" class="run-btn" title="View Data"><i class="fas fa-chart-bar"></i> View</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Email</strong> Breakdown</h3>
        <p>View summary data broken down by email category.</p>
        <div class="controls">
          <a href="${request.contextPath}/admin/eventSummaryBreakdownEmail" class="run-btn" title="View Data"><i class="fas fa-chart-pie"></i> View</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Email and Entity</strong> Breakdown</h3>
        <p>View summary data broken down by email category and entity.</p>
        <div class="controls">
          <a href="${request.contextPath}/admin/eventSummaryBreakdownEmailEntity" class="run-btn" title="View Data"><i class="fas fa-chart-pie"></i> View</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Reason</strong> Breakdown</h3>
        <p>View summary data broken down by reason type.</p>
        <div class="controls">
          <a href="${request.contextPath}/admin/eventSummaryBreakdownReason" class="run-btn" title="View Data"><i class="fas fa-chart-pie"></i> View</a>
        </div>
      </div>

      <div class="service-card">
        <h3><strong>Reason and Entity</strong> Breakdown</h3>
        <p>View summary data broken down by reason type and entity.</p>
        <div class="controls">
          <a href="${request.contextPath}/admin/eventSummaryBreakdownReasonEntity" class="run-btn" title="View Data"><i class="fas fa-chart-pie"></i> View</a>
        </div>
      </div>
    </div>
  </div>
</div>
</body>
</html>
