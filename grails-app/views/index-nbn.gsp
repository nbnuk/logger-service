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
            $('#startYearMonthPicker').datepicker({
                format: "yyyy-mm",
                viewMode: "months",
                minViewMode: "months",
                autoclose: true
            });

            $('#endYearMonthPicker').datepicker({
                format: "yyyy-mm",
                viewMode: "months",
                minViewMode: "months",
                autoclose: true
            });

            // Set default values last year/month
            var today = new Date();
            var currentMonthYear = (today.getFullYear() - 1) + '-' + (today.getMonth() + 1).toString().padStart(2, '0');
            var nextMonthYear = today.getFullYear() + '-' + ((today.getMonth() + 2) % 12 || 12).toString().padStart(2, '0');

            $('#startYearMonth').val(currentMonthYear);
            $('#endYearMonth').val(nextMonthYear);

            // Add form submission handler to format dates correctly
            $('#download-form').submit(function(e) {
                // Get the date values
                var startDate = $('#startYearMonth').val();
                var endDate = $('#endYearMonth').val();

                // Convert from YYYY-MM to YYYYMM format
                if (startDate) {
                    var formattedStart = startDate.replace('-', '');
                    $('input[name="fromMonth"]').val(formattedStart);
                }

                if (endDate) {
                    var formattedEnd = endDate.replace('-', '');
                    $('input[name="toMonth"]').val(formattedEnd);
                }

            });
        });
    </script>

</head>
<body>
<div class="container">
	<h1>NBN Logger web services</h1>
</div>
<div class="album py-5 bg-light">
    <div class="container">
      <div class="row">
        <div class="col-md-4">
          <div class="panel panel-primary mb-4 mt-10 mx-10 shadow-sm">
            <div class="panel-body p-4">
            <p class="panel-text">Summarises user activity by month. It counts the number of unique users, total events, and total associated records for a specific event type within a the date range.</p>


              <div class="d-flex justify-content-between align-items-center"></div>
                <form id="download-form" class="form-horizontal px-3" action="${request.contextPath}/service/downloadsByMonthCSV" method="GET">


                    <div class="form-group">
                        <div class="row">
                            <div class="col-sm-6">
                                <label for="startYearMonth" class="control-label">Start Year Month</label>
                                <div class="input-group date mb-3 mx-3" id="startYearMonthPicker">
                                    <input type="text" class="form-control" name="fromMonth" id="startYearMonth" placeholder="YYYY-MM" required >
                                    <span class="input-group-addon">
                                        <span class="glyphicon glyphicon-calendar"></span>
                                    </span>
                                </div>
                            </div>
                            <div class="col-sm-6">
                                <label for="endYearMonth" class="control-label">End Year Month</label>
                                <div class="input-group date mb-3 mx-3" id="endYearMonthPicker">
                                    <input type="text" class="form-control" name="toMonth" id="endYearMonth" placeholder="YYYY-MM" required >
                                    <span class="input-group-addon">
                                        <span class="glyphicon glyphicon-calendar"></span>
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="row">
                            <div class="col-sm-6">
                                <label for="eventId" class="control-label">Event Type ID</label>
                                <input type="text" class="form-control mb-3 mx-3" name="eventId" id="eventTypeId" value="1002" required>
                            </div>
                            <div class="col-sm-6">
                                <label for="excludeReasonTypeId" class="control-label">Exclude Reason Type ID</label>
                                <input type="text" class="form-control mb-3 mx-3" name="excludeReasonTypeId" id="excludeReasonTypeId" value="10">
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="row">
                            <div class="col-sm-12">
                                <button type="submit" class="btn btn-primary btn-block mx-3">Download CSV</button>
                            </div>
                        </div>
                    </div>
                </form>
            </div>

          </div>
        </div>



        </div>
      </div>
    </div>
  </div>
</body>
</html>
