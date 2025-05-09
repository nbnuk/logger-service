<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <meta name="layout" content="${grailsApplication.config.skin.layout}" />
  <meta name="breadcrumb" content="NBN Logger Service" />
  <title>NBN Logger service | ${grailsApplication.config.skin.orgNameLong}</title>

  <!-- Bootstrap Datepicker Plugin -->
  <link rel="stylesheet" href="https://unpkg.com/bootstrap-datepicker@1.9.0/dist/css/bootstrap-datepicker3.min.css">
  <script src="https://unpkg.com/bootstrap-datepicker@1.9.0/dist/js/bootstrap-datepicker.min.js"></script>

  <script type="text/javascript">
    $(document).ready(function() {
      // Initialize datepickers
      $('.datepicker').datepicker({
        format: "yyyy-mm",
        viewMode: "months",
        minViewMode: "months",
        autoclose: true
      });

      // Set default values: last year to current month
      var today = new Date();
      var currentMonthYear = (today.getFullYear() - 1) + '-' + (today.getMonth() + 1).toString().padStart(2, '0');
      var nextMonthYear = today.getFullYear() + '-' + ((today.getMonth() + 2) % 12 || 12).toString().padStart(2, '0');

      // Set default date range values
      $('.start-month').val(currentMonthYear);
      $('.end-month').val(nextMonthYear);

      // Format date values before form submission
      $('.download-form').submit(function(e) {
        // Get the date values
        var startDate = $(this).find('.start-month').val();
        var endDate = $(this).find('.end-month').val();

        // Convert from YYYY-MM to YYYYMM format
        if (startDate) {
          var formattedStart = startDate.replace('-', '');
          $(this).find('input[name="from"]').val(formattedStart);
        }

        if (endDate) {
          var formattedEnd = endDate.replace('-', '');
          $(this).find('input[name="to"]').val(formattedEnd);
        }

        // Continue with form submission
        return true;
      });
    });
  </script>
</head>
<body>
<div class="container">
  <h1>NBN Logger Service Administration</h1>
  <div class="row">
    <div class="col-md-12">
      <div class="panel panel-default">
        <div class="panel-heading">
          <h3 class="panel-title">Download Logger Data</h3>
        </div>
        <div class="panel-body">
          <div class="row">
            <!-- Monthly Events Download Form -->
            <div class="col-md-6">
              <div class="well">
                <h4 class="text-primary">Monthly Download Statistics</h4>
                <p class="text-muted">Download a report of all downloads broken down by month. Shows total download events and record counts for each month in the selected period. Testing downloads are excluded by default.</p>
                <form class="download-form" action="${request.contextPath}/admin/nbn/download/events" method="GET">
                  <div class="form-group">
                    <label>Start Month</label>
                    <div class="input-group date">
                      <input type="text" class="form-control input-sm start-month datepicker" placeholder="YYYY-MM" required>
                      <input type="hidden" name="from">
                      <span class="input-group-addon">
                        <span class="glyphicon glyphicon-calendar"></span>
                      </span>
                    </div>
                  </div>
                  <div class="form-group">
                    <label>End Month</label>
                    <div class="input-group date">
                      <input type="text" class="form-control input-sm end-month datepicker" placeholder="YYYY-MM" required>
                      <input type="hidden" name="to">
                      <span class="input-group-addon">
                        <span class="glyphicon glyphicon-calendar"></span>
                      </span>
                    </div>
                  </div>
                  <div class="form-group">
                    <div class="row">
                      <div class="col-xs-6">
                        <label>Format</label>
                        <select name="format" class="form-control input-sm">
                          <option value="csv" selected>CSV</option>
                          <option value="json">JSON</option>
                        </select>
                      </div>
                      <div class="col-xs-6">
                        <label>&nbsp;</label>
                        <button type="submit" class="btn btn-primary btn-sm btn-block">
                          Download <i class="glyphicon glyphicon-download"></i>
                        </button>
                      </div>
                    </div>
                  </div>
                  <input type="hidden" name="eventId" value="1002">
                  <input type="hidden" name="excludeReasonTypeId" value="10">
                </form>
              </div>
            </div>

            <!-- Reason Category Download Form -->
            <div class="col-md-6">
              <div class="well">
                <h4 class="text-primary">Downloads by Reason</h4>
                <p class="text-muted">Download a report of all downloads broken down by reason category and month. Shows what reasons users selected when downloading data during the selected period.</p>
                <form class="download-form" action="${request.contextPath}/admin/nbn/download/reasons" method="GET">
                  <div class="form-group">
                    <label>Start Month</label>
                    <div class="input-group date">
                      <input type="text" class="form-control input-sm start-month datepicker" placeholder="YYYY-MM" required>
                      <input type="hidden" name="from">
                      <span class="input-group-addon">
                        <span class="glyphicon glyphicon-calendar"></span>
                      </span>
                    </div>
                  </div>
                  <div class="form-group">
                    <label>End Month</label>
                    <div class="input-group date">
                      <input type="text" class="form-control input-sm end-month datepicker" placeholder="YYYY-MM" required>
                      <input type="hidden" name="to">
                      <span class="input-group-addon">
                        <span class="glyphicon glyphicon-calendar"></span>
                      </span>
                    </div>
                  </div>
                  <div class="form-group">
                    <div class="row">
                      <div class="col-xs-6">
                        <label>Format</label>
                        <select name="format" class="form-control input-sm">
                          <option value="csv" selected>CSV</option>
                          <option value="json">JSON</option>
                        </select>
                      </div>
                      <div class="col-xs-6">
                        <label>&nbsp;</label>
                        <button type="submit" class="btn btn-primary btn-sm btn-block">
                          Download <i class="glyphicon glyphicon-download"></i>
                        </button>
                      </div>
                    </div>
                  </div>
                  <input type="hidden" name="eventId" value="1002">
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
</body>
</html>