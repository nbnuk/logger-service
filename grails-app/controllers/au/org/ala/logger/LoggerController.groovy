package au.org.ala.logger

import grails.converters.JSON
import grails.plugins.csv.CSVWriter
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import org.ala.client.model.LogEventVO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import groovy.time.*

import javax.ws.rs.Consumes
import javax.ws.rs.Path
import javax.ws.rs.Produces
import java.text.DateFormat
import java.text.SimpleDateFormat

import static io.swagger.v3.oas.annotations.enums.ParameterIn.PATH
import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY


class LoggerController {

    final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For"
    final String USER_AGENT_HEADER = "user-agent"
    final String UNCLASSIFIED_REASON_TYPE = "unclassified"
    final String UNCLASSIFIED_SOURCE_TYPE = "unclassified"
    final List<String> EMAIL_CATEGORIES = ["edu", "gov", "other", "unspecified"]

    def loggerService

    def index = {
        render(view: "/index")
    }

    def indexNbn = {
        render(view: "/index-nbn")
    }

    def notAuthorised = {}

    /**
     * Create a new event log record. Record details are expected in the JSON object of the POST request in the form of
     * a JSON-rendered {@link LogEventVO}
     * <p/>
     * Example POST url: <pre>.../logger</pre>
     *
     * @return JSON representation of the new log record.
     */
    @Operation(
            method = "POST",
            tags = "logger",
            operationId = "Create a new Event log",
            summary = "Create a new Event log",
            description = "Create a new Event log",
            requestBody = @RequestBody(
                    description = "The created new Event log",
                    required = true,
                    content = @Content(
                            mediaType = 'application/json',
                            schema = @Schema(implementation = LogEventVO)
                    )
            ),
            responses = [
                    @ApiResponse(
                            description = "Create a new Event log",
                            responseCode = "200"
                    )
            ]
    )
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/service/logger")
    def save() {
        String ip = request.getHeader(X_FORWARDED_FOR_HEADER) ?: request.getRemoteAddr()
        ip = ip.tokenize(", ")[0] // Sometimes see 2 IP addresses like '3.105.55.111, 3.105.55.111' - grab first value
        log.debug("Received log event from remote host ${request.getRemoteHost()} with ip address ${ip}")

        String userAgent = request.getHeader(USER_AGENT_HEADER) ?: "MOZILLA 5.0"

        // ignore any JSON attribute that is not a property of the LogEventVO class to avoid constructor errors
        List fields = LogEventVO.properties.declaredFields.collect { it.name }
        Map json = request.getJSON().findAll { k, v -> fields.contains(k) && k != "class"}

        LogEventVO incomingLog = new LogEventVO(json);
        Map props = [realIp: ip, userAgent: userAgent]
        log.debug "incomingLog = ${incomingLog} || props = ${props}"
        log.debug "Checking loggerService is not null = ${(loggerService != null)}"

        try {
            LogEvent logEvent = loggerService.createLog(incomingLog, props)
            //log.debug("rendering json: ${logEvent.toJSON()}")
            render logEvent.toJSON()
        } catch (Exception e) {
            handleError(HttpStatus.NOT_ACCEPTABLE, "Failed to create log entry", e)
        }
    }

    /**
     * Retrieve a single specific log event in JSON format. Expects params.id to contain the event log id to search for.
     * <p/>
     * Example url: <pre>.../logger/1</pre>
     *
     * @return JSON representation of the specified event log, or HTTP 404 if no matching record is found
     */
    @Operation(
            method = "GET",
            tags = "logger",
            operationId = "Get Event log",
            summary = "Get Event log",
            description = "Get Event log",
            parameters = [
                    @Parameter(
                            name = "id",
                            in = PATH,
                            description = "Event ID",
                            schema = @Schema(implementation = Long),
                            required = true
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Get Event log",
                            responseCode = "200"
                    )
            ]
    )
    @Path("/service/logger/{id}")
    @Produces("application/json")
    def getEventLog() {
        def logEvent = loggerService.findLogEvent(params.id as long)

        if (!logEvent) {
            handleError(HttpStatus.NOT_FOUND, "No matching log event of id ${params.id} was found")
        } else {
            render logEvent.toJSON()
        }
    }

    /**
     * Retrieve a monthly breakdown of log events in the form [month: recordCount], queried by event type id and entity uid.
     * <p/>
     * The request is expected to have the following parameters:
     * <ul>
     * <li>eventTypeId - the event type to query on. Mandatory.
     * <li>q - the entityUid to query on. Mandatory.
     * <li>year - the month pattern to group by. Optional. Defaults to the current year.
     * </ul>
     * <p/>
     * Example url: <pre>.../logger/get.json?q=123&entityTypeId=2&year=201403</pre>
     * <p/>
     * Example response: <pre>{"months":[["201401",123],["201403",3211],["201404",32]]}</pre>get
     *
     * @return monthly breakdown of log events in the form [month: recordCount]
     */
    @Operation(
            method = "GET",
            tags = "logger",
            operationId = "Get Monthly Breakdown",
            summary = "Get Monthly Breakdown",
            description = "Get Monthly Breakdown",
            parameters = [
                    @Parameter(
                            name = "eventTypeId",
                            in = QUERY,
                            description = "Event Type ID",
                            schema = @Schema(implementation = Integer),
                            required = true
                    ),
                    @Parameter(
                            name = "q",
                            in = QUERY,
                            description = "The entityUid to query on",
                            schema = @Schema(implementation = String),
                            required = true
                    ),
                    @Parameter(
                            name = "year",
                            in = QUERY,
                            description = "year",
                            schema = @Schema(implementation = String),
                            required = false
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Get Monthly Breakdown",
                            responseCode = "200"
                    )
            ]
    )
    @Path("/service/logger/get.json")
    @Produces("application/json")
    def monthlyBreakdown() {
        if (!params.q || !params.eventTypeId) {
            handleError(HttpStatus.BAD_REQUEST, "Request is missing either q (entityUid) or eventTypeId")
        } else {
            String year = params.year ?: Calendar.getInstance().get(Calendar.YEAR) as String
            log.debug "monthlyBreakdown() - ${params.eventTypeId}, ${params.q}, ${year}"
            // the 'q' URL request parameter corresponds to the entityUid field
            def monthlyBreakdown = loggerService.getLogEventCount(params.eventTypeId, params.q, year);

            render ([months: monthlyBreakdown] as JSON)
        }
    }

    /**
     * Retrieve a breakdown of log events by reason for a particular entity
     * <p/>
     * The request is expected to have the following parameters:
     * <ul>
     * <li>eventId - the event<strong>Type</strong>Id to query on. Mandatory.</li>
     * <li>entityUid - the entityUid to query on. Mandatory.</li>
     * </ul>
     * <p/>
     * Example url: <pre>.../logger/getReasonBreakdown?eventId=1002&entityUid=in4</pre>
     *
     * @return breakdown of log events by reason in JSON format
     */
    @Operation(
            method = "GET",
            tags = "logger",
            operationId = "Get Reason Breakdown",
            summary = "Get Reason Breakdown",
            description = "Get Reason Breakdown",
            parameters = [
                    @Parameter(
                            name = "eventId",
                            in = QUERY,
                            description = "Event ID",
                            schema = @Schema(implementation = Integer),
                            required = true
                    ),
                    @Parameter(
                            name = "entityUid",
                            in = QUERY,
                            description = "EntityUID",
                            schema = @Schema(implementation = String),
                            required = true
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Get Reason Breakdown",
                            responseCode = "200"
                    )
            ]
    )
    @Path("/service/reasonBreakdown")
    def getReasonBreakdown() {
        // Validate mandatory parameters
        if (!params.eventId) {
            return handleError(HttpStatus.BAD_REQUEST, "Request is missing eventId")
        }
        // entityUid is optional in some service calls, but seems required here based on old logic
        if (!params.entityUid) {
             return handleError(HttpStatus.BAD_REQUEST, "Request is missing entityUid")
        }

        // Read optional parameters
        String dateRangeParam = params.'date-range' ?: "All Time" // Default to "All Time" if not provided
        String formatParam = params.format ?: "JSON" // Default to JSON

        log.debug("getReasonBreakdown called with eventId: ${params.eventId}, entityUid: ${params.entityUid}, date-range: ${dateRangeParam}, format: ${formatParam}")

        // Calculate date range using helper
        Map dateRange = calculateDateRange(dateRangeParam)
        Date fromDate = dateRange.fromDate
        Date toDate = dateRange.toDate

        // Fetch data for the calculated period
        Map<Integer, String> reasonMap = getReasonMap() // Helper method already exists
        // Call the existing helper which calls the service
        // Note: getReasonBreakdownForPeriod returns [events: totalEvents, records: totalRecords, reasonBreakdown: grouped]
        def periodData = getReasonBreakdownForPeriod(params.eventId, params.entityUid, fromDate, toDate, reasonMap)

        // Render based on format
        if (formatParam.equalsIgnoreCase("CSV")) {
            log.debug("Rendering Reason Breakdown as CSV")
            response.contentType = "text/csv"
            // Use entityUid if present, otherwise 'all' - adjust filename slightly from original CSV action
            String filename = "reason-breakdown-${params.entityUid ?: 'all'}-${dateRangeParam.toLowerCase().replace(' ', '-')}.csv"
            response.addHeader("Content-Disposition", "attachment; filename=\"${filename}\"")

            // Extract the actual breakdown map: [ReasonName:[events:X, records:Y], ...]
            def breakdownMap = periodData?.reasonBreakdown ?: [:]

            if (breakdownMap) {
                 // Use a writer that transforms the map into rows
                 // We need Reason, Events, Records
                 // Note: The original CSV action (getReasonBreakdownCSV) fetched monthly data differently.
                 // This new CSV will output totals per reason for the selected period.
                def csv = new CSVWriter(response.writer, {
                    col1: "reason"         { it.key } // Reason Name is the key
                    col2: "number of events" { it.value.events }
                    col3: "number of records"{ it.value.records }
                })
                // Sort by reason name for consistent output
                breakdownMap.sort { it.key }.each { reason, data -> csv << [key: reason, value: data] }

            } else {
                 // Header for empty CSV
                response.writer.write("\"reason\",\"number of events\",\"number of records\"")
            }
            response.writer.flush()

        } else { // Default to JSON
            log.debug("Rendering Reason Breakdown as JSON")
            // Render the data structure returned by getReasonBreakdownForPeriod
            // This includes totals and the breakdown map
            render(contentType: "application/json") {
                 delegate.reasonBreakdown periodData
            }
        }
    }

    /**
     * Retrieve a breakdown of log events by source for a particular entity
     * <p/>
     * The request is expected to have the following parameters:
     * <ul>
     * <li>eventId - the event<strong>Type</strong>Id to query on. Mandatory.</li>
     * <li>entityUid - the entityUid to query on. Mandatory.</li>
     * <li>excludeReasonTypeId - the <code>logReasonTypeId</code> to exclude from results (usually &quot;testing&quot;)</li>
     * </ul>
     * <p/>
     * Example url: <pre>.../logger/getReasonBreakdown?eventId=1002&entityUid=in4</pre>
     *
     * @return breakdown of log events by reason in JSON format
     */
    @Operation(
            method = "GET",
            tags = "logger",
            operationId = "Get Source Breakdown",
            summary = "Get Source Breakdown",
            description = "Get Source Breakdown",
            parameters = [
                    @Parameter(
                            name = "eventId",
                            in = QUERY,
                            description = "Event ID",
                            schema = @Schema(implementation = Integer),
                            required = true
                    ),
                    @Parameter(
                            name = "entityUid",
                            in = QUERY,
                            description = "EntityUID",
                            schema = @Schema(implementation = String),
                            required = true
                    ),
                    @Parameter(
                            name = "excludeReasonTypeId",
                            in = QUERY,
                            description = "Exclude Reason Type ID",
                            schema = @Schema(implementation = Integer),
                            required = false
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Get Source Breakdown",
                            responseCode = "200"
                    )
            ]
    )
    @Path("/service/sourceBreakdown")
    def getSourceBreakdown() {
        // Validate mandatory parameters
        if (!params.eventId) {
            return handleError(HttpStatus.BAD_REQUEST, "Request is missing eventId")
        }
        if (!params.entityUid) {
             return handleError(HttpStatus.BAD_REQUEST, "Request is missing entityUid")
        }

        // Read optional parameters
        String dateRangeParam = params.'date-range' ?: "All Time" // Default to "All Time" if not provided
        String formatParam = params.format ?: "JSON" // Default to JSON
        Integer excludeReasonTypeId = params.int("excludeReasonTypeId") // Keep existing optional param

        log.debug("getSourceBreakdown called with eventId: ${params.eventId}, entityUid: ${params.entityUid}, date-range: ${dateRangeParam}, format: ${formatParam}, excludeReasonTypeId: ${excludeReasonTypeId}")

        // Calculate date range using helper
        Map dateRange = calculateDateRange(dateRangeParam)
        Date fromDate = dateRange.fromDate
        Date toDate = dateRange.toDate

        // Fetch data for the calculated period
        Map<Integer, String> sourceMap = getSourceMap()
        // Call the existing helper which calls the service
        // Note: getSourceBreakdownForPeriod returns [events: totalEvents, records: totalRecords, sourceBreakdown: grouped]
        def periodData = getSourceBreakdownForPeriod(params.eventId, params.entityUid, fromDate, toDate, sourceMap, excludeReasonTypeId)

        // Render based on format
        if (formatParam.equalsIgnoreCase("CSV")) {
            log.debug("Rendering Source Breakdown as CSV")
            response.contentType = "text/csv"
            String filename = "source-breakdown-${params.entityUid ?: 'all'}-${dateRangeParam.toLowerCase().replace(' ', '-')}.csv"
            response.addHeader("Content-Disposition", "attachment; filename=\"${filename}\"")

            // Extract the actual breakdown map: [SourceName:[events:X, records:Y], ...]
            def breakdownMap = periodData?.sourceBreakdown ?: [:]

            if (breakdownMap) {
                 // Use a writer that transforms the map into rows
                 // We need Source, Events, Records
                def csv = new CSVWriter(response.writer, {
                    col1: "source"         { it.key } // Source Name is the key
                    col2: "number of events" { it.value.events }
                    col3: "number of records"{ it.value.records }
                })
                // Sort by source name for consistent output
                breakdownMap.sort { it.key }.each { source, data -> csv << [key: source, value: data] }

            } else {
                 // Header for empty CSV
                response.writer.write("\"source\",\"number of events\",\"number of records\"")
            }
            response.writer.flush()

        } else { // Default to JSON
            log.debug("Rendering Source Breakdown as JSON")
            // Render the data structure returned by getSourceBreakdownForPeriod
            render(contentType: "application/json") {
                delegate.sourceBreakdown periodData
            }
        }
    }

    /**
     * Retrieve a monthly breakdown of log events for downloads
     * <p/>
     * The request is expected to have the following parameters:
     * <ul>
     *     <li>eventId - the event <strong>type</strong> to query on. Mandatory.
     *     <li>entityUid - the entity id to search for.
     *     <li>reasonTypeId - the log reason to query on. Optional. If not provided, all reasons will be included
     *     <li>sourceTypeId - the log source to query on. Optional. If not provided, all sources will be included
     *     <li>excludeReasonTypeId - the <code>logReasonTypeId</code> to exclude from results (usually &quot;testing&quot;)
     * </ul>
     * Example request: <pre>.../logger/sourceBreakdownMonthly?eventId=1002&entityUid=in4&reasonId=1</pre>
     * <p/>
     * Example response: <pre>{"temporalBreakdown":{"201212":{"records":344,"events":1},"201311":{"records":1188,"events":4}}}</pre>
     *
     * @return breakdown of log events by month in JSON format
     */
    @Operation(
            method = "GET",
            tags = "logger",
            operationId = "Get Reason Breakdown by Month",
            summary = "Get Reason Breakdown by Month",
            description = "Get Reason Breakdown by Month",
            parameters = [
                    @Parameter(
                            name = "eventId",
                            in = QUERY,
                            description = "Event ID",
                            schema = @Schema(implementation = Integer),
                            required = true
                    ),
                    @Parameter(
                            name = "entityUid",
                            in = QUERY,
                            description = "EntityUID",
                            schema = @Schema(implementation = String),
                            required = true
                    ),
                    @Parameter(
                            name = "reasonId",
                            in = QUERY,
                            description = "Reason ID",
                            schema = @Schema(implementation = Integer),
                            required = false
                    ),
                    @Parameter(
                            name = "sourceId",
                            in = QUERY,
                            description = "Source ID",
                            schema = @Schema(implementation = Integer),
                            required = false
                    ),
                    @Parameter(
                            name = "excludeReasonTypeId",
                            in = QUERY,
                            description = "Exclude Reason Type ID",
                            schema = @Schema(implementation = Integer),
                            required = false
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Get Reason Breakdown by Month",
                            responseCode = "200"
                    )
            ]
    )
    @Path("/service/reasonBreakdownMonthly")
    def getReasonBreakdownByMonth() {
        // Validate mandatory parameters
        if (!params.eventId) {
            return handleError(HttpStatus.BAD_REQUEST, "Request is missing eventId")
        }
        if (!params.entityUid) {
            return handleError(HttpStatus.BAD_REQUEST, "Request is missing entityUid")
        }

        // Read optional parameters
        String dateRangeParam = params.'date-range' ?: "All Time"
        String formatParam = params.format ?: "JSON"
        String reasonId = params.reasonId // Keep existing optional param
        String sourceId = params.sourceId // Keep existing optional param
        Integer excludeReasonTypeId = params.int("excludeReasonTypeId") // Keep existing optional param

        log.debug("getReasonBreakdownByMonth called with eventId: ${params.eventId}, entityUid: ${params.entityUid}, reasonId: ${reasonId}, sourceId: ${sourceId}, date-range: ${dateRangeParam}, format: ${formatParam}, excludeReasonTypeId: ${excludeReasonTypeId}")

        // Calculate date range and convert to strings
        Map dateRange = calculateDateRange(dateRangeParam)
        String fromDateStr = getYearAndMonth(dateRange.fromDate) // Use helper
        String toDateStr = getYearAndMonth(dateRange.toDate)     // Use helper (will be exclusive in comparison logic)

        log.debug("Calculated date range: from ${fromDateStr} (inclusive) to ${toDateStr} (exclusive)")

        // Fetch data (currently fetches all months based on other filters)
        def results
        if (sourceId) {
            results = loggerService.getTemporalEventsSourceBreakdown(params.eventId, params.entityUid, reasonId, sourceId, excludeReasonTypeId)
        } else {
            results = loggerService.getTemporalEventsReasonBreakdown(params.eventId, params.entityUid, reasonId, excludeReasonTypeId)
        }

        // Filter results by date range if applicable
        List filteredResults = results
        if (fromDateStr && toDateStr) {
            filteredResults = results?.findAll { it.month >= fromDateStr && it.month < toDateStr }
        }
        log.debug("Found ${results?.size()} total months, ${filteredResults?.size()} months after filtering by date range")

        // Render based on format
        if (formatParam.equalsIgnoreCase("CSV")) {
            log.debug("Rendering Reason Breakdown by Month as CSV")
            response.contentType = "text/csv"
            String filename = "reason-breakdown-monthly-${params.entityUid ?: 'all'}-${dateRangeParam.toLowerCase().replace(' ', '-')}.csv"
            response.addHeader("Content-Disposition", "attachment; filename=\"${filename}\"")

            if (filteredResults) {
                // Use logic similar to original getReasonBreakdownByMonthCSV
                def csv = new CSVWriter(response.writer, {
                    col1: "year-month"       { it.month }
                    col2: "year"             { it.month?.substring(0, 4) }
                    col3: "month"            { it.month?.substring(4, 6) }
                    col4: "number of events" { it.numberOfEvents }
                    col5: "number of records"{ it.recordCount }
                })
                // Sort by month
                filteredResults.sort { it.month }.each { e -> csv << e }
            } else {
                response.writer.write("\"year-month\",\"year\",\"month\",\"number of events\",\"number of records\"")
            }
            response.writer.flush()

        } else { // Default to JSON
            log.debug("Rendering Reason Breakdown by Month as JSON")
            // Convert the filtered list of summaries into a map keyed by month
            def grouped = filteredResults ? filteredResults.collectEntries { [(it.month): [records: it.recordCount, events: it.numberOfEvents]] } : [:]
            render(contentType: "application/json") {
                 delegate.temporalBreakdown grouped
            }
        }
    }

    /**
     * Retrieve a breakdown of log events by email category for a particular entity
     * <p/>
     * The request is expected to have the following parameters:
     * <ul>
     * <li>eventId - the event<strong>Type</strong>Id to query on. Mandatory.
     * <li>entityUid - the entityUid to query on. Mandatory.
     * </ul>
     * <p/>
     * Example url: <pre>.../logger/getReasonBreakdown?eventId=1002&entityUid=in4</pre>
     *
     * @return breakdown of log events by reason in JSON format
     */
    @Operation(
            method = "GET",
            tags = "logger",
            operationId = "Get Email Breakdown",
            summary = "Get Email Breakdown",
            description = "Get Email Breakdown",
            parameters = [
                    @Parameter(
                            name = "eventId",
                            in = QUERY,
                            description = "Event ID",
                            schema = @Schema(implementation = Integer),
                            required = true
                    ),
                    @Parameter(
                            name = "entityUid",
                            in = QUERY,
                            description = "EntityUID",
                            schema = @Schema(implementation = String),
                            required = true
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Get Email Breakdown",
                            responseCode = "200"
                    )
            ]
    )
    @Produces("application/json")
    @Path("/service/emailBreakdown")
    def getEmailBreakdown() {
        // Validate mandatory parameters
        if (!params.eventId) {
            return handleError(HttpStatus.BAD_REQUEST, "Request is missing eventId")
        }
        // Should we mandate entityUid like the others?
        if (!params.entityUid) {
             return handleError(HttpStatus.BAD_REQUEST, "Request is missing entityUid")
        }

        // Read optional parameters
        String dateRangeParam = params.'date-range' ?: "All Time"
        String formatParam = params.format ?: "JSON"

        log.debug("getEmailBreakdown called with eventId: ${params.eventId}, entityUid: ${params.entityUid}, date-range: ${dateRangeParam}, format: ${formatParam}")

        // Calculate date range using helper
        Map dateRange = calculateDateRange(dateRangeParam)
        Date fromDate = dateRange.fromDate
        Date toDate = dateRange.toDate

        // Fetch data for the calculated period
        // Note: getEmailBreakdownForPeriod returns [events: totalEvents, records: totalRecords, emailBreakdown: grouped]
        def periodData = getEmailBreakdownForPeriod(params.eventId, params.entityUid, fromDate, toDate)

        // Render based on format
        if (formatParam.equalsIgnoreCase("CSV")) {
            log.debug("Rendering Email Breakdown as CSV")
            response.contentType = "text/csv"
            String filename = "email-breakdown-${params.entityUid ?: 'all'}-${dateRangeParam.toLowerCase().replace(' ', '-')}.csv"
            response.addHeader("Content-Disposition", "attachment; filename=\"${filename}\"")

            // Extract the actual breakdown map: [CategoryName:[events:X, records:Y], ...]
            def breakdownMap = periodData?.emailBreakdown ?: [:]

            if (breakdownMap) {
                def csv = new CSVWriter(response.writer, {
                    col1: "user category"    { it.key } // Category Name is the key
                    col2: "number of events" { it.value.events }
                    col3: "number of records"{ it.value.records }
                })
                // Sort by category name for consistent output
                breakdownMap.sort { it.key }.each { category, data -> csv << [key: category, value: data] }
            } else {
                response.writer.write("\"user category\",\"number of events\",\"number of records\"")
            }
            response.writer.flush()

        } else { // Default to JSON
            log.debug("Rendering Email Breakdown as JSON")
            // Render the data structure returned by getEmailBreakdownForPeriod
            render(contentType: "application/json") {
                delegate.emailBreakdown periodData
            }
        }
    }

    /**
     * Requests are in the format /{entityUid}/events/{eventId}/counts.
     *  <p/>
     *  Optional param:
     *  <ul>
     *    <li>excludeReasonTypeId - the <code>logReasonTypeId</code> to exclude from results (usually &quot;testing&quot;). Optional. If not provided, all reasons will be included
     *  </ul>
     *  Example request: <pre>.../logger/dr143/events/1024/counts.json</pre>
     */
    def getEntityBreakdown() {
        use(TimeCategory) {
            Date nextMonth = nextMonth()
            Integer excludeReasonTypeId = params.int("excludeReasonTypeId")

            def results = [:]
            results << ["all": getEntityBreakdownForPeriod(params.eventId, params.entityUid, null, null, excludeReasonTypeId)]
            results << ["last3Months": getEntityBreakdownForPeriod(params.eventId, params.entityUid, nextMonth - 3.months, nextMonth, excludeReasonTypeId)]
            results << ["thisMonth": getEntityBreakdownForPeriod(params.eventId, params.entityUid, nextMonth - 1.month, nextMonth, excludeReasonTypeId)]
            results << ["lastYear": getEntityBreakdownForPeriod(params.eventId, params.entityUid, nextMonth - 12.months, nextMonth, excludeReasonTypeId)]

            render results as JSON
        }
    }

    /**
     * Retrieve a breakdown of log events by event type
     * <p/>
     * Example request: <pre>.../logger/totalsByType
     * <p/>
     * Example response: <pre>{"1000":{"records":2706,"events":62},...}</pre>
     *
     * @return breakdown of log events by event type in JSON format
     */
    @Operation(
            method = "GET",
            tags = "logger",
            operationId = "Get Totals by Event Type",
            summary = "Get Totals by Event Type",
            description = "Get Totals by Event Type",
            responses = [
                    @ApiResponse(
                            description = "Get Totals by Event Type",
                            responseCode = "200"
                    )
            ]
    )
    @Path("/service/totalsByType")
    @Produces("application/json")
    def getTotalsByEventType() {
        def results = loggerService.getEventTypeBreakdown()

        // convert the list of summaries into a map keyed by the category (month) so it can be rendered in the desired JSON formats
        def grouped = results.collectEntries { [(it.logEventTypeId): [records: it.recordCount, events: it.numberOfEvents]] }

        render ([totals: grouped] as JSON)
    }

    /**
     * List all log event types
     * <p/>
     * Example url: <pre>.../logger/events</pre>
     *
     * @return all log event types in JSON format
     */
    @Operation(
            method = "GET",
            tags = "logger",
            operationId = "Get Event Types",
            summary = "Get Event Types",
            description = "Get Event Types",
            responses = [
                    @ApiResponse(
                            description = "Get Event Types",
                            responseCode = "200"
                    )
            ]
    )
    @Path("/service/logger/events")
    @Produces("application/json")
    def getEventTypes() {
        render loggerService.getAllEventTypes().collect({k -> [name: k.name, id: k.id]}) as JSON
    }

    /**
     * List all log reason types
     * <p/>
     * Example url: <pre>.../logger/reasons</pre>
     *
     * @return all log reason types in JSON format
     */
    @Operation(
            method = "GET",
            tags = "logger",
            operationId = "Get Reason Types",
            summary = "Get Reason Types",
            description = "Get Reason Types",
            responses = [
                    @ApiResponse(
                            description = "Get Reason Types",
                            responseCode = "200"
                    )
            ]
    )
    @Path("/service/logger/reasons")
    @Produces("application/json")
    def getReasonTypes() {
        def json = loggerService.getAllReasonTypes().collect({k -> [rkey: k.rkey, name: k.name, id: k.id, deprecated: k.isDeprecated]}) as JSON
        log.debug "getReasonTypes = ${json}"
        render json
    }

    /**
     * List all log source types
     * <p/>
     * Example url: <pre>.../logger/sources</pre>
     *
     * @return all log source types in JSON format
     */
    @Operation(
            method = "GET",
            tags = "logger",
            operationId = "Get Source Types",
            summary = "Get Source Types",
            description = "Get Source Types",
            responses = [
                    @ApiResponse(
                            description = "Get Source Types",
                            responseCode = "200"
                    )
            ]
    )
    @Path("/service/logger/sources")
    @Produces("application/json")
    def getSourceTypes() {
        render loggerService.getAllSourceTypes().collect({k -> [name: k.name, id: k.id]}) as JSON
    }

    // returns a triple of [totalEvents | totalRecords | emailBreakdown] for the requested period.
    private def getEmailBreakdownForPeriod(eventTypeId, entityUid, from, to) {
        def emailSummary = loggerService.getEventsEmailBreakdown(eventTypeId as int, entityUid, getYearAndMonth(from), getYearAndMonth(to))

        def grouped = EMAIL_CATEGORIES.collectEntries { v -> [(v): ["events": 0, "records": 0]] }

        def totalEvents = 0
        def totalRecords = 0

        if (emailSummary) {
            emailSummary.each {
                def entry = grouped[it.userEmailCategory]
                entry["records"] += it.recordCount
                entry["events"] += it.numberOfEvents
                totalEvents += it.numberOfEvents
                totalRecords += it.recordCount
            }
        }

        [events: totalEvents, records: totalRecords, emailBreakdown: grouped]
    }

    // returns a triple of [totalEvents | totalRecords | reasonBreakdown] for the requested period.
    private def getReasonBreakdownForPeriod(eventTypeId, entityUid, from, to, reasonMap) {
        def reasonSummary = loggerService.getEventsReasonBreakdown(eventTypeId as int, entityUid, getYearAndMonth(from), getYearAndMonth(to))

        def grouped = reasonMap.collectEntries { k, v -> [(v): ["events": 0, "records": 0]] }
                .withDefault { ["events": 0, "records": 0] }

        def totalEvents = 0
        def totalRecords = 0

        if (reasonSummary) {
            reasonSummary.each {
                def entry = grouped[reasonMap[it.logReasonTypeId] ?: UNCLASSIFIED_REASON_TYPE]
                entry["records"] += it.recordCount
                entry["events"] += it.numberOfEvents
                totalEvents += it.numberOfEvents
                totalRecords += it.recordCount
            }
        }

        [events: totalEvents, records: totalRecords, reasonBreakdown: grouped]
    }

    // returns a triple of [totalEvents | totalRecords | sourceBreakdown] for the requested period.
    private def getSourceBreakdownForPeriod(eventTypeId, entityUid, from, to, sourceMap, Integer excludeReasonTypeId ) {
        def sourceSummary = loggerService.getEventsSourceBreakdown(eventTypeId as int, entityUid, getYearAndMonth(from), getYearAndMonth(to), excludeReasonTypeId)

        def grouped = sourceMap.collectEntries { k, v -> [(v): ["events": 0, "records": 0]] }
                .withDefault { ["events": 0, "records": 0] }

        def totalEvents = 0
        def totalRecords = 0

        if (sourceSummary) {
            sourceSummary.each {
                def entry = grouped[sourceMap[it.logSourceTypeId] ?: UNCLASSIFIED_SOURCE_TYPE]
                entry["records"] += it.recordCount
                entry["events"] += it.numberOfEvents
                totalEvents += it.numberOfEvents
                totalRecords += it.recordCount
            }
        }

        [events: totalEvents, records: totalRecords, sourceBreakdown: grouped]
    }

    // returns a tuple of [totalEvents | totalRecords] for the requested period.
    private def getEntityBreakdownForPeriod(eventTypeId, entityUid, from, to, excludeReasonTypeId) {
        def entitySummary = loggerService.getLogEventsByEntity(eventTypeId as int, entityUid, getYearAndMonth(from), getYearAndMonth(to), excludeReasonTypeId)

        def totalEvents = 0
        def totalRecords = 0

        if (entitySummary) {
            entitySummary.each {
                totalEvents += it.numberOfEvents
                totalRecords += it.recordCount
            }
        }

        [numberOfEvents: totalEvents, numberOfEventItems: totalRecords]
    }

    private def handleError(HttpStatus httpStatus, String logMessage, Throwable e = null) {
        log.error(logMessage, e)
        response.setStatus(httpStatus.value())
        render(status: httpStatus.value(), text: logMessage)
    }

    private getReasonMap() {
        Map<Integer, String> reasonMap = loggerService.getAllReasonTypes().collectEntries({
            [it.id as Integer, it.name]
        })
        reasonMap
    }

    private getSourceMap() {
        Map<Integer, String> sourceMap = loggerService.getAllSourceTypes().collectEntries({
            [it.id as Integer, it.name]
        })
        sourceMap
    }

    /**
     * Returns first day of next month
     * @return Date
     */
    private Date nextMonth() {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.DAY_OF_MONTH, 1);
        Date nextMonth = date.getTime() + 1.month
        nextMonth
    }

    /**
     * Returns year and month of a Date
     * @param inDate Date passed in
     * @return String of yyyyMM
     */
    private String getYearAndMonth(Date inDate) {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMM")
        String outDate = inDate? dateFormat.format(inDate) : inDate
        outDate
    }

    /**
     * Calculates the start and end dates based on a date range string parameter.
     * @param dateRangeParam The string representing the date range (e.g., "Last Month", "Last 3 Months", "Last 1 Year", "All Time").
     * @return A Map containing [fromDate: Date, toDate: Date]. Returns null dates for "All Time" or invalid input.
     */
    private Map calculateDateRange(String dateRangeParam) {
        Date fromDate = null
        Date toDate = null
        use(TimeCategory) {
            Date nextMon = nextMonth() // Use existing helper
            switch (dateRangeParam) {
                case "Last Month":
                    fromDate = nextMon - 1.month
                    toDate = nextMon
                    break
                case "Last 3 Months":
                    fromDate = nextMon - 3.months
                    toDate = nextMon
                    break
                case "Last 1 Year":
                    fromDate = nextMon - 12.months
                    toDate = nextMon
                    break
                case "All Time":
                    // fromDate and toDate remain null
                    break
                default:
                    log.warn("Invalid date-range parameter received: ${dateRangeParam}. Defaulting to All Time.")
                    // fromDate and toDate remain null
                    break
            }
        }
        return [fromDate: fromDate, toDate: toDate]
    }

}
