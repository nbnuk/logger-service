package au.org.ala.logger

import grails.testing.web.controllers.ControllerUnitTest
import groovy.json.JsonOutput
import groovy.time.TimeCategory
import org.springframework.http.HttpStatus
import spock.lang.Specification
import spock.lang.Unroll
import au.org.ala.logger.LogSourceType
import grails.converters.JSON

import javax.persistence.PersistenceException
import java.text.DateFormat
import java.text.SimpleDateFormat

class LoggerControllerSpec extends Specification implements ControllerUnitTest<LoggerController> {

    def VALID_JSON_REQUEST = """{ "eventTypeId": 1000, "comment":"test comment", "userEmail" : "fred@somewhere.gov.au", "userIP": "123.123.123.123", "recordCounts" : { "uid1": 100, "uid2": 200,} }""";

    private LoggerController controller
    private LoggerService loggerService
    private reasonTypes = []
    private sourceTypes = []

    private String thisMonth
    private String nextMonth
    private String last3Months
    private String last12Months

    def setup() {
        controller = new LoggerController();
        loggerService = Mock(LoggerService)
        controller.loggerService = loggerService

        for (i in 1..10) {
            def reason = new LogReasonType(name: "reason${i}", rkey: "rkey${i}")
            reason.setId(i)
            reasonTypes << reason
        }
        loggerService.getAllReasonTypes() >> reasonTypes

        for (i in 1..5) {
            def source = new LogSourceType(name: "source${i}")
            source.setId(i)
            sourceTypes << source
        }
        loggerService.getAllSourceTypes() >> sourceTypes

        use(TimeCategory) {
            thisMonth = getYearAndMonth(new Date())
            nextMonth = getYearAndMonth(new Date() + 1.month)
            last3Months = getYearAndMonth(new Date() - 2.months)
            last12Months = getYearAndMonth(new Date() - 11.months)

            def fixedNextMonthDate = new GregorianCalendar(2023, Calendar.APRIL, 1).time
            controller.metaClass.nextMonth = { -> fixedNextMonthDate }
        }

        controller.metaClass.getReasonMap = { -> reasonTypes.collectEntries{[(it.id): it.name]} }
        controller.metaClass.getSourceMap = { -> sourceTypes.collectEntries{[(it.id): it.name]} }
    }

    def cleanup() {
    }

    def "index action should render the index view"() {
        when:
        controller.index()
        then:
        view == '/index'
    }

    def "indexNbn action should render the index-nbn view"() {
        when:
        controller.indexNbn()
        then:
        view == '/index-nbn'
    }

    def "save() should ignore any JSON attributes that do not match properties of the LogEventVO object"() {
        when: "the incoming JSON contains an attribute 'class'"
        request.json = """{ "class": "myclassname", lastUpdated: "aaa", "eventTypeId": 1000, "comment":"test comment", "userEmail" : "fred@somewhere.gov.au", "userIP": "123.123.123.123", "recordCounts" : { "uid1": 100, "uid2": 200,} }"""
        controller.save()

        then: "the system should ignore that attribute to avoid ReadOnlyPropertyExceptions"
        1 * loggerService.createLog(!null, !null) >> new LogEvent()
        assert response.status == HttpStatus.OK.value()
    }

    def "save() should invoke the logger service save method"() {
        when: "the controller is sent a valid JSON request for a new log event"
        request.json = VALID_JSON_REQUEST
        controller.save()

        then: "the logger service createLog method should be called"
        1 * loggerService.createLog(!null, !null)
    }

    def "save() should return a HTTP 406 (not acceptable) if an exception occurs while saving"() {
        when: "an exception is thrown from the logger service's createLog method"
        request.json = VALID_JSON_REQUEST
        loggerService.createLog(*_) >> { throw new PersistenceException("test") }
        controller.save()

        then: "a http 406 (NOT_ACCEPTABLE) should be returned"
        assert response.status == HttpStatus.NOT_ACCEPTABLE.value()
    }

    def "save() should pull the 'user-agent' header from the request and save it with the log_event"() {
        when: "a request is made to save a new log event"
        request.addHeader("user-agent", "someUserAgent")
        request.json = VALID_JSON_REQUEST
        controller.save()

        then: "the user-agent header should be passed to the service as an additional parameter"
        1 * loggerService.createLog(*_) >> { arguments ->
            Map additionalParamsArg = arguments[1]
            assert additionalParamsArg["userAgent"] == "someUserAgent"
        }
    }

    def "Find log event should return 404 when no match is found"() {
        when: "there is no matching log event"
        params.id = 10
        loggerService.findLogEvent(_) >> null
        controller.getEventLog()

        then: "a http 404 should be returned"
        assert response.status == HttpStatus.NOT_FOUND.value()
    }

    def "Find log event should return a JSON view of the matching event"() {
        when: "there is a matching log event"
        params.id = 10
        LogEvent log = new LogEvent();
        log.setComment("test comment")
        log.setId(1)
        log.setLogEventTypeId(1)
        log.setLogReasonTypeId(10)
        log.setLogSourceTypeId(20)
        log.setMonth("201411")
        log.setSource("someSource")
        log.setSourceUrl("http://some.source.com")
        log.setUserEmail("fred@somewhere.gov.au")
        log.setUserIp("123.123.123.123")
        LogDetail detail1 = new LogDetail()
        detail1.setId(1)
        detail1.setEntityType("type1")
        detail1.setEntityUid("uid1")
        detail1.setLogEvent(log)
        detail1.setRecordCount(100)
        log.logDetails.add(detail1)

        loggerService.findLogEvent(_) >> log
        controller.getEventLog();

        then: "the log event should be returned in JSON format"
        assert response.json.logEvent.userEmail == "fred@somewhere.gov.au"
        assert response.json.logEvent.userIp == "123.123.123.123"
        assert response.json.logEvent.logEventTypeId == 1
        assert response.json.logEvent.logReasonTypeId == 10
        assert response.json.logEvent.logSourceTypeId == 20
        assert response.json.logEvent.sourceUrl == "http://some.source.com"
        assert response.json.logEvent.month == "201411"
        assert response.json.logEvent.source == "someSource"
        assert response.json.logEvent.comment == "test comment"
        assert response.json.logEvent.id: 1
        assert response.json.logEvent.logDetails[0].entityUid == "uid1"
        assert response.json.logEvent.logDetails[0].recordCount == 100
        assert response.json.logEvent.logDetails[0].entityType == "type1"
        assert response.json.logEvent.logDetails[0].id == 1
    }

    def "monthlyBreakdown requires request parameter 'q' for the entityUid"() {
        when: "a request is made with no 'q' parameter"
        params.eventTypeId = 12
        controller.monthlyBreakdown()

        then: "a http 400 (BAD_REQUEST) should be returned"
        assert response.status == HttpStatus.BAD_REQUEST.value()
    }

    def "monthlyBreakdown requires request parameter 'entityTypeId'"() {
        when: "a request is made with no 'eventTypeId' parameter"
        params.q = 12
        controller.monthlyBreakdown()

        then: "a http 400 (BAD_REQUEST) should be returned"
        assert response.status == HttpStatus.BAD_REQUEST.value()
    }

    def "monthlyBreakdown should default the year parameter to the current year if not provided"() {
        def thisYear = Calendar.getInstance().get(Calendar.YEAR) as String

        when: "a request is made with no 'year' parameter"
        params.q = 12
        params.eventTypeId = 1
        loggerService.getLogEventCount(_, _, _) >> [[201401, 123], [201403, 3211], [201404, 32]]
        controller.monthlyBreakdown()

        then: "then the controller should default the parameter to the current year"
        1 * loggerService.getLogEventCount(_, _, thisYear)
    }

    def "monthlyBreakdown returns a list of monthly record numbers"() {
        when: "a request is made with valid parameters"
        params.q = 12
        params.eventTypeId = 1
        params.year = "2014"
        loggerService.getLogEventCount(_, _, _) >> [["201401", 123], ["201403", 3211], ["201404", 32]]
        controller.monthlyBreakdown()

        then: "a valid response should be returned"
        assert response.status == HttpStatus.OK.value()
        assert response.json.months[0][0] == "201401" && response.json.months[0][1] == 123
        assert response.json.months[1][0] == "201403" && response.json.months[1][1] == 3211
        assert response.json.months[2][0] == "201404" && response.json.months[2][1] == 32
    }

    def "getReasonBreakdown should return 400 if eventId is missing"() {
        when:
        params.entityUid = "dr143"
        controller.getReasonBreakdown()

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        response.text == "Request is missing eventId"
    }

    def "getReasonBreakdown should return 400 if entityUid is missing"() {
        when:
        params.eventId = 1000
        controller.getReasonBreakdown()

        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        response.text == "Request is missing entityUid"
    }

    @Unroll
    def "getReasonBreakdown should call service with correct dates for #dateRangeParam"() {
        given:
        params << [entityUid: "dr143", eventId: 1000, 'date-range': dateRangeParam]

        // Calculate dates here using TimeCategory
        Date expectedFromDate
        Date expectedToDate
        use(TimeCategory) {
            expectedToDate = monthsToSubtract != null ? controller.nextMonth() : null
            expectedFromDate = monthsToSubtract != null ? controller.nextMonth() - monthsToSubtract.months : null
        }

        // Mock service response (needed for successful execution)
        loggerService.getEventsReasonBreakdown(params.eventId as int, params.entityUid, getYearAndMonth(expectedFromDate), getYearAndMonth(expectedToDate)) >> []

        when:
        controller.getReasonBreakdown()

        then:
        1 * loggerService.getEventsReasonBreakdown(params.eventId as int, params.entityUid, getYearAndMonth(expectedFromDate), getYearAndMonth(expectedToDate))
        response.status == HttpStatus.OK.value()

        where:
        dateRangeParam    | monthsToSubtract
        "Last Month"      | 1
        "Last 3 Months"   | 3
        "Last 1 Year"     | 12
        "All Time"        | null
        "Invalid Range"   | null // Defaults to All Time
        null              | null // Defaults to All Time
    }

    def "getReasonBreakdown should render JSON by default or when format=JSON"() {
        given:
        params << [entityUid: "dr143", eventId: 1000, format: formatParam]
        def mockReasonSummaries = [new EventSummaryBreakdownReason(logReasonTypeId: 1, numberOfEvents: 3, recordCount: 30),
                                   new EventSummaryBreakdownReason(logReasonTypeId: 2, numberOfEvents: 7, recordCount: 70)]
        loggerService.getEventsReasonBreakdown(_, _, _, _) >> mockReasonSummaries

        when:
        controller.getReasonBreakdown()

        then:
        response.status == HttpStatus.OK.value()
        response.contentType == 'application/json;charset=UTF-8'
        response.json.reasonBreakdown.events == 10
        response.json.reasonBreakdown.records == 100
        response.json.reasonBreakdown.reasonBreakdown.reason1.events == 3
        response.json.reasonBreakdown.reasonBreakdown.reason2.records == 70
        response.json.reasonBreakdown.reasonBreakdown.reason3 // Check a reason not in mock data has default 0 counts
        response.json.reasonBreakdown.reasonBreakdown.reason3.events == 0
        response.json.reasonBreakdown.reasonBreakdown.reason3.records == 0

        where:
        formatParam << [null, "JSON", "json", "InvalidFormat"]
    }

    def "getReasonBreakdown should render CSV when format=CSV"() {
        given:
        params << [entityUid: "dr143", eventId: 1000, format: "CSV", 'date-range': "Last Month"]
        def mockReasonSummaries = [new EventSummaryBreakdownReason(logReasonTypeId: 1, numberOfEvents: 3, recordCount: 30),
                                   new EventSummaryBreakdownReason(logReasonTypeId: 2, numberOfEvents: 7, recordCount: 70)]
        loggerService.getEventsReasonBreakdown(_, _, _, _) >> mockReasonSummaries

        // Mock the response writer
        def stringWriter = new StringWriter()
        response.writer = new PrintWriter(stringWriter)

        when:
        controller.getReasonBreakdown()

        then:
        response.status == HttpStatus.OK.value()
        response.contentType == 'text/csv'
        response.getHeader('Content-Disposition').contains('filename="reason-breakdown-dr143-last-month.csv"')
        // Check CSV content (headers + data rows)
        def csvOutput = stringWriter.toString().readLines()
        csvOutput.size() == 3 // Header + 2 data rows
        csvOutput[0] == '"reason","number of events","number of records"'
        csvOutput[1] == '"reason1","3","30"' // Sorted by name
        csvOutput[2] == '"reason2","7","70"'
    }

    def "getSourceBreakdown should return 400 if eventId is missing"() {
        when:
        params.entityUid = "dr143"
        controller.getSourceBreakdown()
        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        response.text == "Request is missing eventId"
    }

    def "getSourceBreakdown should return 400 if entityUid is missing"() {
        when:
        params.eventId = 1000
        controller.getSourceBreakdown()
        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        response.text == "Request is missing entityUid"
    }

    @Unroll
    def "getSourceBreakdown should call service with correct dates for #dateRangeParam"() {
        given:
        params << [entityUid: "dr143", eventId: 1000, 'date-range': dateRangeParam, excludeReasonTypeId: 5]

        // Calculate dates here using TimeCategory
        Date expectedFromDate
        Date expectedToDate
        use(TimeCategory) {
            expectedToDate = monthsToSubtract != null ? controller.nextMonth() : null
            expectedFromDate = monthsToSubtract != null ? controller.nextMonth() - monthsToSubtract.months : null
        }

        loggerService.getEventsSourceBreakdown(params.eventId as int, params.entityUid, getYearAndMonth(expectedFromDate), getYearAndMonth(expectedToDate), params.excludeReasonTypeId as int) >> []

        when:
        controller.getSourceBreakdown()

        then:
        1 * loggerService.getEventsSourceBreakdown(params.eventId as int, params.entityUid, getYearAndMonth(expectedFromDate), getYearAndMonth(expectedToDate), params.excludeReasonTypeId as int)
        response.status == HttpStatus.OK.value()

        where:
        dateRangeParam    | monthsToSubtract
        "Last Month"      | 1
        "All Time"        | null
        null              | null
    }

    def "getSourceBreakdown should render JSON by default or when format=JSON"() {
        given:
        params << [entityUid: "dr143", eventId: 1000, format: formatParam]
        def mockSourceSummaries = [[logSourceTypeId: 1, numberOfEvents: 1, recordCount: 10],
                                   [logSourceTypeId: 2, numberOfEvents: 2, recordCount: 20]]
        loggerService.getEventsSourceBreakdown(_, _, _, _, _) >> mockSourceSummaries

        when:
        controller.getSourceBreakdown()

        then:
        response.status == HttpStatus.OK.value()
        response.contentType == 'application/json;charset=UTF-8'
        response.json.sourceBreakdown.events == 3
        response.json.sourceBreakdown.records == 30
        response.json.sourceBreakdown.sourceBreakdown.source1.events == 1
        response.json.sourceBreakdown.sourceBreakdown.source2.records == 20

        where:
        formatParam << [null, "JSON", "json", "InvalidFormat"]
    }

    def "getSourceBreakdown should render CSV when format=CSV"() {
        given:
        params << [entityUid: "dr143", eventId: 1000, format: "CSV", 'date-range': "All Time"]
        def mockSourceSummaries = [[logSourceTypeId: 1, numberOfEvents: 1, recordCount: 10],
                                   [logSourceTypeId: 2, numberOfEvents: 2, recordCount: 20]]
        loggerService.getEventsSourceBreakdown(_, _, _, _, _) >> mockSourceSummaries
        def stringWriter = new StringWriter()
        response.writer = new PrintWriter(stringWriter)

        when:
        controller.getSourceBreakdown()

        then:
        response.status == HttpStatus.OK.value()
        response.contentType == 'text/csv'
        response.getHeader('Content-Disposition').contains('filename="source-breakdown-dr143-all-time.csv"')
        def csvOutput = stringWriter.toString().readLines()
        csvOutput.size() == 3
        csvOutput[0] == '"source","number of events","number of records"'
        csvOutput[1] == '"source1","1","10"'
        csvOutput[2] == '"source2","2","20"'
    }

    def "getReasonBreakdownByMonth should return 400 if eventId is missing"() {
        when:
        params.entityUid = "dr143"
        controller.getReasonBreakdownByMonth()
        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        response.text == "Request is missing eventId"
    }

    def "getReasonBreakdownByMonth should return 400 if entityUid is missing"() {
        when:
        params.eventId = 1000
        controller.getReasonBreakdownByMonth()
        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        response.text == "Request is missing entityUid"
    }

    @Unroll
    def "getReasonBreakdownByMonth should call service and filter results for JSON format with #dateRangeParam"() {
        given: "Mock service returns data spanning multiple months"
        params << [entityUid: "dr143", eventId: 1000, 'date-range': dateRangeParam, format: "JSON"]
        def fullResults = [
            new EventSummaryBreakdownReason(month: "202301", numberOfEvents: 1, recordCount: 10),
            new EventSummaryBreakdownReason(month: "202302", numberOfEvents: 2, recordCount: 20),
            new EventSummaryBreakdownReason(month: "202303", numberOfEvents: 3, recordCount: 30)
        ]
        // Mock service call - it's called without date range, filtering happens after
        loggerService.getTemporalEventsReasonBreakdown(params.eventId as int, params.entityUid, null, null) >> fullResults

        when:
        controller.getReasonBreakdownByMonth()

        then:
        1 * loggerService.getTemporalEventsReasonBreakdown(params.eventId as int, params.entityUid, null, null)
        response.status == HttpStatus.OK.value()
        response.contentType == 'application/json;charset=UTF-8'
        JsonOutput.toJson(response.json) // Ensure valid JSON
        response.json.temporalBreakdown.size() == expectedMonths // Check if filtering worked
        if (expectedMonths == 1) {
            assert response.json.temporalBreakdown."202303".records == 30
        }

        where:
        dateRangeParam    | expectedMonths
        "Last Month"      | 1 // March 2023
        "Last 3 Months"   | 3 // Jan, Feb, Mar 2023
        "Last 1 Year"     | 3 // Jan, Feb, Mar 2023 (only 3 months available in mock data)
        "All Time"        | 3
        null              | 3
    }

    @Unroll
    def "getReasonBreakdownByMonth should call service and filter results for CSV format with #dateRangeParam"() {
        given:
        params << [entityUid: "dr143", eventId: 1000, format: "CSV", 'date-range': dateRangeParam]
        def fullResults = [
            new EventSummaryBreakdownReason(month: "202301", numberOfEvents: 1, recordCount: 10),
            new EventSummaryBreakdownReason(month: "202302", numberOfEvents: 2, recordCount: 20),
            new EventSummaryBreakdownReason(month: "202303", numberOfEvents: 3, recordCount: 30)
        ]
        loggerService.getTemporalEventsReasonBreakdown(params.eventId as int, params.entityUid, null, null) >> fullResults

        def stringWriter = new StringWriter()
        response.writer = new PrintWriter(stringWriter)

        when:
        controller.getReasonBreakdownByMonth()

        then:
        1 * loggerService.getTemporalEventsReasonBreakdown(params.eventId as int, params.entityUid, null, null)
        response.status == HttpStatus.OK.value()
        response.contentType == 'text/csv'
        response.getHeader('Content-Disposition').contains("reason-breakdown-monthly-dr143-${dateRangeParam?.toLowerCase()?.replace(' ','-') ?: 'all-time'}.csv")
        def csvOutput = stringWriter.toString().readLines()
        csvOutput.size() == expectedRows + 1 // Header + data rows

        where:
        dateRangeParam    | expectedRows
        "Last Month"      | 1
        "Last 3 Months"   | 3
        "Last 1 Year"     | 3
        "All Time"        | 3
        null              | 3
    }

    def "getEmailBreakdown should return 400 if eventId is missing"() {
        when:
        params.entityUid = "dr143"
        controller.getEmailBreakdown()
        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        response.text == "Request is missing eventId"
    }

    def "getEmailBreakdown should return 400 if entityUid is missing"() {
        when:
        params.eventId = 1000
        controller.getEmailBreakdown()
        then:
        response.status == HttpStatus.BAD_REQUEST.value()
        response.text == "Request is missing entityUid"
    }

    @Unroll
    def "getEmailBreakdown should call service with correct dates for #dateRangeParam"() {
        given:
        params << [entityUid: "dr143", eventId: 1000, 'date-range': dateRangeParam]

        // Calculate dates here using TimeCategory
        Date expectedFromDate
        Date expectedToDate
        use(TimeCategory) {
            expectedToDate = monthsToSubtract != null ? controller.nextMonth() : null
            expectedFromDate = monthsToSubtract != null ? controller.nextMonth() - monthsToSubtract.months : null
        }

        loggerService.getEventsEmailBreakdown(params.eventId as int, params.entityUid, getYearAndMonth(expectedFromDate), getYearAndMonth(expectedToDate)) >> []

        when:
        controller.getEmailBreakdown()

        then:
        1 * loggerService.getEventsEmailBreakdown(params.eventId as int, params.entityUid, getYearAndMonth(expectedFromDate), getYearAndMonth(expectedToDate))
        response.status == HttpStatus.OK.value()

        where:
        dateRangeParam    | monthsToSubtract
        "Last Month"      | 1
        "All Time"        | null
    }

    def "getEmailBreakdown should render JSON by default or when format=JSON"() {
        given:
        params << [entityUid: "dr143", eventId: 1000, format: formatParam]
        def mockEmailSummaries = [new EventSummaryBreakdownEmail(userEmailCategory: "edu", numberOfEvents: 5, recordCount: 50)]
        loggerService.getEventsEmailBreakdown(_, _, _, _) >> mockEmailSummaries

        when:
        controller.getEmailBreakdown()

        then:
        response.status == HttpStatus.OK.value()
        response.contentType == 'application/json;charset=UTF-8'
        response.json.emailBreakdown.events == 5
        response.json.emailBreakdown.records == 50
        response.json.emailBreakdown.emailBreakdown.edu.events == 5
        response.json.emailBreakdown.emailBreakdown.gov.events == 0 // Check default zero count
        response.json.emailBreakdown.emailBreakdown.other.events == 0
        response.json.emailBreakdown.emailBreakdown.unspecified.events == 0

        where:
        formatParam << [null, "JSON", "json", "InvalidFormat"]
    }

    def "getEmailBreakdown should render CSV when format=CSV"() {
        given:
        params << [entityUid: "dr143", eventId: 1000, format: "CSV", 'date-range': "Last 3 Months"]
        def mockEmailSummaries = [new EventSummaryBreakdownEmail(userEmailCategory: "edu", numberOfEvents: 5, recordCount: 50),
                                  new EventSummaryBreakdownEmail(userEmailCategory: "other", numberOfEvents: 2, recordCount: 20)]
        loggerService.getEventsEmailBreakdown(_, _, _, _) >> mockEmailSummaries
        def stringWriter = new StringWriter()
        response.writer = new PrintWriter(stringWriter)

        when:
        controller.getEmailBreakdown()

        then:
        response.status == HttpStatus.OK.value()
        response.contentType == 'text/csv'
        response.getHeader('Content-Disposition').contains('filename="email-breakdown-dr143-last-3-months.csv"')
        def csvOutput = stringWriter.toString().readLines()
        csvOutput.size() == 3 // Header + 2 data rows
        csvOutput[0] == '"user category","number of events","number of records"'
        csvOutput[1] == '"edu","5","50"'
        csvOutput[2] == '"other","2","20"'
    }

    def "getEntityBreakdown should look for this month, last 3 months, last 12 months and all time"() {
        when: "a breakdown is requested"
        params << [entityUid: "dr143", eventId: 1000]
        loggerService.getLogEventsByEntity(_, _, _, _, _) >> []
        controller.getEntityBreakdown()

        then: "the service method should be invoked 4 times with the relevant date ranges"
        def fromThisMonth = getYearAndMonth(controller.nextMonth() - 1.month)
        def fromLast3 = getYearAndMonth(controller.nextMonth() - 3.months)
        def fromLastYear = getYearAndMonth(controller.nextMonth() - 12.months)
        def toNextMonth = getYearAndMonth(controller.nextMonth())

        1 * loggerService.getLogEventsByEntity(1000, "dr143", null, null, null) // all time
        1 * loggerService.getLogEventsByEntity(1000, "dr143", fromThisMonth, toNextMonth, null) // this month
        1 * loggerService.getLogEventsByEntity(1000, "dr143", fromLast3, toNextMonth, null) // last 3 months
        1 * loggerService.getLogEventsByEntity(1000, "dr143", fromLastYear, toNextMonth, null) // last 12 months
    }

    def "getEntityBreakdown should collate results from difference date ranges correctly"() {
        when: "a breakdown is requested"
        params << [entityUid: "dr143", eventId: 1000]
        loggerService.getLogEventsByEntity(_, _, null, null, null) >> [new EventSummaryBreakdownReason(numberOfEvents: 3, recordCount: 30)]
        loggerService.getLogEventsByEntity(_, _, thisMonth, nextMonth, null) >> [new EventSummaryBreakdownReason(numberOfEvents: 6, recordCount: 40)]
        loggerService.getLogEventsByEntity(_, _, last3Months, nextMonth, null) >> [new EventSummaryBreakdownReason(numberOfEvents: 8, recordCount: 50)]
        loggerService.getLogEventsByEntity(_, _, last12Months, nextMonth, null) >> [new EventSummaryBreakdownReason(numberOfEvents: 10, recordCount: 60)]
        controller.getEntityBreakdown()

        then: "the results should be collated properly"
        assert response.json.all.numberOfEvents == 3 && response.json.all.numberOfEventItems == 30
        assert response.json.last3Months.numberOfEvents == 8 && response.json.last3Months.numberOfEventItems == 50
        assert response.json.thisMonth.numberOfEvents == 6 && response.json.thisMonth.numberOfEventItems == 40
        assert response.json.lastYear.numberOfEvents == 10 && response.json.lastYear.numberOfEventItems == 60
    }

    def "getAllEventTypes should return event types in the correct JSON format"() {
        when:
        def event1 = new LogEventType(name: "event1")
        event1.setId(1)
        def event2 = new LogEventType(name: "event2")
        event2.setId(2)
        def event3 = new LogEventType(name: "event3")
        event3.setId(3)

        loggerService.getAllEventTypes() >> [event1, event2, event3]
        controller.getEventTypes()

        then:
        assert response.text == """[{"name":"event1","id":1},{"name":"event2","id":2},{"name":"event3","id":3}]"""
    }

    def "getAllSourceTypes should return event types in the correct JSON format"() {
        when:
        def source1 = new LogSourceType(name: "source1")
        source1.setId(1)
        def source2 = new LogSourceType(name: "source2")
        source2.setId(2)
        def source3 = new LogSourceType(name: "source3")
        source3.setId(3)

        loggerService.getAllSourceTypes() >> [source1, source2, source3]
        controller.getSourceTypes()

        then:
        assert response.text == """[{"name":"source1","id":1},{"name":"source2","id":2},{"name":"source3","id":3}]"""
    }

    def "getAllReasonTypes should return event types in the correct JSON format"() {
        loggerService = Mock(LoggerService)
        controller.loggerService = loggerService

        when:
        def reason1 = new LogReasonType(name: "reason1", rkey: "key1")
        reason1.setId(1)
        def reason2 = new LogReasonType(name: "reason2", rkey: "key2")
        reason2.setId(2)
        def reason3 = new LogReasonType(name: "reason3", rkey: "key3")
        reason3.setId(3)

        loggerService.getAllReasonTypes() >> [reason1, reason2, reason3]
        controller.getReasonTypes()

        then:
        assert response.text == """[{"rkey":"key1","name":"reason1","id":1,"deprecated":false},{"rkey":"key2","name":"reason2","id":2,"deprecated":false},{"rkey":"key3","name":"reason3","id":3,"deprecated":false}]"""
    }

    private String getYearAndMonth(Date inDate) {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMM")
        String outDate = inDate? dateFormat.format(inDate) : inDate
        outDate
    }
}
