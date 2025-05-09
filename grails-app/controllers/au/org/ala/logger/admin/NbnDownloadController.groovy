package au.org.ala.logger.admin

import au.org.ala.logger.EventSummaryBreakdownReason
import au.org.ala.web.AlaSecured
import grails.converters.JSON
import grails.plugins.csv.CSVWriter
import org.springframework.http.HttpStatus

// You may want to temporarily commented out the security check for development testing
// @AlaSecured(value = "ROLE_ADMIN", redirectController = 'logger', redirectAction = 'notAuthorised')
class NbnDownloadController {

    def loggerService

    /**
     * Generate data containing a monthly breakdown of log events for downloads
     * <p/>
     * The request is expected to have the following parameters:
     * <ul>
     *     <li>eventId - the logEventTypeId to query on. Mandatory.
     *     <li>entityUid - the entity id to search for. Optional.
     *     <li>reasonTypeId - the log reason to query on. Optional. If not provided, all reasons will be included
     *     <li>sourceTypeId - the log source to query on. Optional. If not provided, all sources will be included
     *     <li>excludeReasonTypeId - the <code>logReasonTypeId</code> to exclude from results (usually &quot;testing&quot;). Optional. If not provided, all reasons will be included
     *     <li>from - from yyyyMM. Optional.
     *     <li>to - to yyyyMM. Optional.
     *     <li>format - response format (csv or json). Optional. Default is csv.
     * </ul>
     * Example requests:
     * Downloads totals by month, excluding reason testing (reasonId 10), between 202304 and 202403
     * <pre>.../admin/nbn/download/events?eventId=1002&excludeReasonTypeId=10&from=202304&to=202403</pre>
     *
     * Downloads totals by month, for reason volunteer.research (reasonId 16) between 202304 and 202403
     * <pre>.../admin/nbn/download/events?eventId=1002&reasonTypeId=16&from=202304&to=202403</pre>
     *
     * @return all log events for the specified eventType in CSV or JSON format
     */
    def getEventCountByMonth() {
        if (!params.eventId) {
            handleError(HttpStatus.BAD_REQUEST, "Request is missing eventId")
            return
        }

        String format = params.format?.toLowerCase() ?: 'csv'

        try {
            def results = loggerService.getTemporalEventsWithDateRange(
                    params.int('eventId'),
                    null,
                    null,
                    null,
                    params.from,
                    params.to
            )

            if (format == 'json') {
                def transformedResults = results.collect { row ->
                    [
                        month: row.month,
                        events: row.numberOfEvents,
                        records: row.recordCount
                    ]
                }

                // Sort results by month
                def sortedResults = transformedResults.sort { a, b -> a.month <=> b.month }

                response.contentType = "application/json;charset=utf-8"
                render text: new groovy.json.JsonBuilder(sortedResults).toString()
            } else {
                // For CSV format
                response.setHeader("Content-Disposition", "attachment; filename=\"downloads-monthly-${params.from ?: ''}-${params.to ?: ''}.csv\"")
                response.setContentType("text/csv")

                def writer = response.writer
                def csv = new CSVWriter(writer, {
                    col1: "year-month" { row -> row.month }
                    col2: "year" { row -> row.month.substring(0, 4) }
                    col3: "month" { row -> row.month.substring(4, 6) }
                    col4: "number of events" { row -> row.numberOfEvents }
                    col5: "number of records" { row -> row.recordCount }
                })

                // Sort results by month
                def sortedResults = results.sort { a, b -> a.month <=> b.month }
                sortedResults.each { row -> csv << row }

                writer.flush()
            }
        } catch (Exception e) {
            handleError(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving event data", e)
        }
    }

      /**
     * Generate data containing reason category breakdown
     *
     * <p/>
     * The request is expected to have the following parameters:
     * <ul>
     *     <li>eventId - the logEventTypeId to query on. Mandatory.
     *     <li>from - from yyyyMM. Optional.
     *     <li>to - to yyyyMM. Optional.
     *     <li>format - response format (csv or json). Optional. Default is csv.
     * </ul>
     *
     * @return reason category breakdown data in CSV or JSON format
     */
    def getReasonCategoryBreakdown() {
        if (!params.eventId) {
            handleError(HttpStatus.BAD_REQUEST, "Request is missing eventId")
            return
        }

        String format = params.format?.toLowerCase() ?: 'csv'

        List results
        try {
            results = loggerService.getReasonBreakdownByMonthAndCategory(
                params.int('eventId'),
                params.from,
                params.to
            )
        } catch (Exception e) {
            handleError(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching reason breakdown data", e)
            return
        }

        // Get a map of reason IDs to names
        Map<Integer, String> reasonMap = loggerService.getAllReasonTypes().collectEntries {
            [it.id as Integer, it.name]
        }

        // Transform results to include year and month
        def downloadData = results.collect { row ->
            def monthStr = row[0] as String
            def year = monthStr.substring(0, 4)
            def month = monthStr.substring(4, 6)

            [
                yearMonth: monthStr,
                year: year,
                month: month,
                reasonId: row[1],
                reason: reasonMap[row[1]] ?: "unknown",
                events: row[2],
                records: row[3]
            ]
        }

        // Handle response format
        if (format == 'json') {
            render downloadData as JSON
        } else {
            // CSV format with year and month columns
            response.contentType = "text/csv"
            response.addHeader("Content-Disposition", "attachment; filename=\"reason-breakdown-${params.from}-${params.to}.csv\"")

            if (downloadData) {
                def csv = new CSVWriter(response.writer, {
                    col1: "year_month" { it.yearMonth }
                    col2: "year" { it.year }
                    col3: "month" { it.month }
                    col4: "reason_id" { it.reasonId }
                    col5: "reason" { it.reason }
                    col6: "events" { it.events }
                    col7: "records" { it.records }
                })

                downloadData.each { e -> csv << e }
            } else {
                response.writer.write("\"year_month\",\"year\",\"month\",\"reason_id\",\"reason\",\"events\",\"records\"")
            }

            response.writer.flush()
        }
    }

    private def handleError(HttpStatus httpStatus, String logMessage, Throwable e = null) {
        log.error(logMessage, e)
        response.setStatus(httpStatus.value())
        render(status: httpStatus.value(), text: logMessage)
    }
}
