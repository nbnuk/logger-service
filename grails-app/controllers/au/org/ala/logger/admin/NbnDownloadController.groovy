package au.org.ala.logger.admin

import au.org.ala.web.AlaSecured
import grails.plugins.csv.CSVWriter
import org.springframework.http.HttpStatus

// You may want to temporarily commented out the security check for development testing
@AlaSecured(value = "ROLE_ADMIN", redirectController = 'logger', redirectAction = 'notAuthorised')
class NbnDownloadController {

    def loggerService

    /**
     * Generate a CSV file containing a monthly breakdown of log events for downloads
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
     * </ul>
     * Example requests:
     * Downloads totals by month, excluding reason testing (reasonId 10), between 202304 and 202403
     * <pre>.../logger/reasonBreakdownByMonthCSV?eventId=1002&excludeReasonTypeId=10&from=202304&to=202403</pre>
     *
     * Downloads totals by month, for reason volunteer.research (reasonId 16) between 202304 and 202403
     * <pre>.../logger/reasonBreakdownByMonthCSV?eventId=1002&reasonTypeId=16&from=202304&to=202403</pre>
     *
     * @return all log events for the specified eventType in CSV format
     */
    def getEventCountByMonth() {
        if (!params.eventId) {
            handleError(HttpStatus.BAD_REQUEST, "Request is missing eventId")
        } else {
            def results
            
            results = loggerService.getTemporalEventsWithDateRange(
                params.int('eventId'),
                params.entityUid,
                params.reasonTypeId ? params.int('reasonTypeId') : null,
                params.excludeReasonTypeId ? params.int('excludeReasonTypeId') : null,
                params.from,
                params.to
            )

            response.contentType = "text/csv"
            response.addHeader("Content-Disposition", "attachment; filename=\"downloads-monthly-${params.from}-${params.to}.csv\"")

            if (results) {
                def csv = new CSVWriter(response.writer, {
                    col1:
                    "year-month" { (it.month as String) }
                    col2:
                    "year" { (it.month as String).substring(0, 4) }
                    col3:
                    "month" { (it.month as String).substring(4, 6) }
                    col4:
                    "number of events" { it.numberOfEvents }
                    col5:
                    "number of records" { it.recordCount }
                })

                results.each { e -> csv << e }
            } else {
                response.writer.write("\"year-month\",\"year\",\"month\",\"number of events\",\"number of records\"")
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
