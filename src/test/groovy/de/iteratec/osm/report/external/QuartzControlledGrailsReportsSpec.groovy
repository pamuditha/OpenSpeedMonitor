/* 
* OpenSpeedMonitor (OSM)
* Copyright 2014 iteratec GmbH
* 
* Licensed under the Apache License, Version 2.0 (the "License"); 
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
* 	http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, 
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and 
* limitations under the License.
*/

package de.iteratec.osm.report.external

import de.iteratec.osm.ConfigService
import de.iteratec.osm.InMemoryConfigService
import de.iteratec.osm.OsmConfiguration
import de.iteratec.osm.batch.Activity
import de.iteratec.osm.batch.BatchActivity
import de.iteratec.osm.batch.BatchActivityService
import de.iteratec.osm.batch.BatchActivityUpdaterDummy
import de.iteratec.osm.csi.EventCsiAggregationService
import de.iteratec.osm.csi.JobGroupCsiAggregationService
import de.iteratec.osm.csi.Page
import de.iteratec.osm.csi.PageCsiAggregationService
import de.iteratec.osm.measurement.environment.Browser
import de.iteratec.osm.measurement.environment.Location
import de.iteratec.osm.measurement.schedule.ConnectivityProfile
import de.iteratec.osm.measurement.schedule.JobGroup
import de.iteratec.osm.measurement.schedule.JobGroupService
import de.iteratec.osm.report.chart.AggregationType
import de.iteratec.osm.report.chart.CsiAggregation
import de.iteratec.osm.report.chart.CsiAggregationInterval
import de.iteratec.osm.report.chart.CsiAggregationUtilService
import de.iteratec.osm.report.external.provider.DefaultGraphiteSocketProvider
import de.iteratec.osm.report.external.provider.GraphiteSocketProvider
import de.iteratec.osm.result.MeasuredEvent
import de.iteratec.osm.result.MvQueryParams
import de.iteratec.osm.util.ServiceMocker
import grails.buildtestdata.BuildDataTest
import grails.buildtestdata.mixin.Build
import grails.testing.services.ServiceUnitTest
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Test
import spock.lang.Specification

import static de.iteratec.osm.report.chart.CsiAggregationInterval.*

@Build([JobGroup, CsiAggregation, GraphiteServer, Page, JobGroup, MeasuredEvent, Location, Browser])
class QuartzControlledGrailsReportsSpec extends Specification implements BuildDataTest,
        ServiceUnitTest<MetricReportingService> {

    static final String jobGroupWithServersName = 'csiGroupWithServers'
    static final String jobGroupWithoutServersName = 'csiGroupWithoutServers'
    static final DateTime hourlyDateExpectedToBeSent = new DateTime(2013, 12, 4, 7, 0, 0, DateTimeZone.UTC)
    static final DateTime dailyDateExpectedToBeSent = new DateTime(2013, 12, 4, 0, 0, 0, DateTimeZone.UTC)
    static final DateTime weeklyDateExpectedToBeSent = new DateTime(2013, 11, 29, 0, 0, 0, DateTimeZone.UTC)
    static final Double firstHourlyValueToSend = 23.3d
    static final Double secondHourlyValueToSend = 123.3d
    static final Double firstDailyValueToSend = 12d
    static final Double secondDailyValueToSend = 14.2
    static final Double firstWeeklyValueToSend = 1223d
    static final Double secondWeeklyValueToSend = 13234.2
    static final String pathPrefix = 'wpt'
    static final String pageName = 'pageAggregator'
    static final String eventName = 'event'
    static final String browserName = 'browser'
    static final String locationLocation = 'location'
    ServiceMocker serviceMocker

    MetricReportingService serviceUnderTest
    public MockedGraphiteSocket graphiteSocketUsedInTests

    Closure doWithSpring() {
        return {
            inMemoryConfigService(InMemoryConfigService)
            csiAggregationUtilService(CsiAggregationUtilService)
            configService(ConfigService)
            eventCsiAggregationService(EventCsiAggregationService)
            pageCsiAggregationService(PageCsiAggregationService)
            shopCsiAggregationService(JobGroupCsiAggregationService)
        }
    }

    void setup() {
        serviceUnderTest = service
        serviceUnderTest.csiAggregationUtilService = grailsApplication.mainContext.getBean('csiAggregationUtilService') as CsiAggregationUtilService
        serviceUnderTest.configService = grailsApplication.mainContext.getBean('configService') as ConfigService
        serviceUnderTest.inMemoryConfigService = grailsApplication.mainContext.getBean('inMemoryConfigService') as InMemoryConfigService
        serviceUnderTest.inMemoryConfigService.activateMeasurementsGenerally()
        new OsmConfiguration().save(failOnError: true)
        mockJobGroupDaoService()
        mockGraphiteSocketProvider()
        mockBatchActivityService()
    }

    void setupSpec() {
        mockDomains(CsiAggregationInterval, OsmConfiguration, BatchActivity, ConnectivityProfile)
    }

    void "writing hourly CsiAggregations to Graphite"() {
        given:
        CsiAggregationInterval interval = new CsiAggregationInterval(name: 'hourly', intervalInMinutes: HOURLY).save()

        CsiAggregation firstHmv = getCsiAggregation(interval, AggregationType.MEASURED_EVENT, firstHourlyValueToSend, hourlyDateExpectedToBeSent, '1,2,3')
        CsiAggregation secondHmv = getCsiAggregation(interval, AggregationType.MEASURED_EVENT, secondHourlyValueToSend, hourlyDateExpectedToBeSent, '4,5,6')

        //mocking
        mockEventCsiAggregationService([firstHmv, secondHmv])
        mockPageCsiAggregationService([], HOURLY)
        mockJobGroupCsiAggregationService([], HOURLY)

        when: "we report the CSI value of the last hour"
        DateTime cronjobStartsAt = hourlyDateExpectedToBeSent.plusMinutes(20)
        serviceUnderTest.reportEventCSIValuesOfLastHour(cronjobStartsAt)

        Integer sentInTotal = 2
        List<MockedGraphiteSocket.SentDate> sent = graphiteSocketUsedInTests.sentDates
        List<MockedGraphiteSocket.SentDate> allSentDatesWithoutServername = sent.findAll {it.path.stringValueOfPathName =~ ".+${jobGroupWithoutServersName}.+"}
        List<MockedGraphiteSocket.SentDate> allSentDatesWithServerName = sent.findAll {it.path.stringValueOfPathName =~ ".+${jobGroupWithServersName}.+" }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithExpectedPath = sent.findAll {
            it.path.stringValueOfPathName == ( pathPrefix + '.' + jobGroupWithServersName +
                    '.hourly.' + pageName + '.' + eventName + '.' + browserName + '.' + locationLocation + '.csi' )
        }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithExpectedTimeStamp = sent.findAll { it.timestamp == (hourlyDateExpectedToBeSent.toDate()) }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithFirstValue = sent.findAll { it.value == (firstHourlyValueToSend * 100) }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithSecondValue = sent.findAll { it.value == (secondHourlyValueToSend * 100) }


        then: "only the expected SentDates should appear"
        allSentDatesWithoutServername.size() == 0
        allSentDatesWithServerName.size() == sent.size()

        sentInTotal ==  sent.size()
        allSentDatesWithExpectedPath.size() == sentInTotal
        allSentDatesWithExpectedTimeStamp.size() == sentInTotal
        allSentDatesWithFirstValue.size() == 1
        allSentDatesWithSecondValue.findAll { it.value == (secondHourlyValueToSend * 100) }.size() == 1
    }

    @Test
    void "writing daily PageCsiCsiAggregations to graphite"() {
        given:
        CsiAggregationInterval interval = new CsiAggregationInterval(name: 'daily', intervalInMinutes: DAILY).save()

        CsiAggregation firstDpmv = getCsiAggregation(interval, AggregationType.PAGE, firstDailyValueToSend, dailyDateExpectedToBeSent, '1,2,3')
        CsiAggregation secondDpmv = getCsiAggregation(interval, AggregationType.PAGE, secondDailyValueToSend, dailyDateExpectedToBeSent, '4,5,6')

        //mocking
        mockEventCsiAggregationService([])
        mockPageCsiAggregationService([firstDpmv, secondDpmv], DAILY)
        mockJobGroupCsiAggregationService([], DAILY)

        when: "we report PageCSIValue of the last hour"
        DateTime cronjobStartsAt = dailyDateExpectedToBeSent.plusMinutes(20)
        serviceUnderTest.reportPageCSIValuesOfLastDay(cronjobStartsAt)
        int sentInTotal = 2
        List<MockedGraphiteSocket.SentDate> sent = graphiteSocketUsedInTests.sentDates
        List<MockedGraphiteSocket.SentDate> allSentDatesWithoutServername = sent.findAll {it.path.stringValueOfPathName =~ ".+${jobGroupWithoutServersName}.+"}
        List<MockedGraphiteSocket.SentDate> allSentDatesWithServerName = sent.findAll {it.path.stringValueOfPathName =~ ".+${jobGroupWithServersName}.+" }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithExpectedPath =         sent.findAll {
            it.path.stringValueOfPathName == ( pathPrefix + '.' + jobGroupWithServersName + '.daily.' + pageName + '.csi' )
        }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithExpectedTimeStamp = sent.findAll { dailyDateExpectedToBeSent.toDate() == it.timestamp }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithFirstValue = sent.findAll { it.value == (firstDailyValueToSend * 100) }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithSecondValue = sent.findAll { it.value == (secondDailyValueToSend * 100) }


        then: "only the expected SentDates should appear"
        allSentDatesWithoutServername.size() == 0
        allSentDatesWithServerName.size() == sent.size()
        sent.size() == sentInTotal
        allSentDatesWithExpectedPath.size() == sentInTotal
        allSentDatesWithExpectedTimeStamp.size() == sentInTotal
        allSentDatesWithFirstValue.size() == 1
        allSentDatesWithSecondValue.size() == 1
    }

    void "writing daily ShopCsiCsiAggregations to graphite"() {
        given:
        CsiAggregationInterval interval = new CsiAggregationInterval(name: 'daily', intervalInMinutes: DAILY).save()

        CsiAggregation firstDsmv = getCsiAggregation(interval, AggregationType.JOB_GROUP, firstDailyValueToSend, dailyDateExpectedToBeSent, '1,2,3')
        CsiAggregation secondDsmv = getCsiAggregation(interval, AggregationType.JOB_GROUP, secondDailyValueToSend, dailyDateExpectedToBeSent, '4,5,6')

        //mocking
        mockEventCsiAggregationService([])
        mockPageCsiAggregationService([], DAILY)
        mockJobGroupCsiAggregationService([firstDsmv, secondDsmv], DAILY)

        when: "we report ShopCSIValues of the last hour"
        DateTime cronjobStartsAt = dailyDateExpectedToBeSent.plusMinutes(20)
        serviceUnderTest.reportShopCSIValuesOfLastDay(cronjobStartsAt)
        int sentInTotal = 2
        List<MockedGraphiteSocket.SentDate> sent = graphiteSocketUsedInTests.sentDates
        List<MockedGraphiteSocket.SentDate> allSentDatesWithoutServername = sent.findAll {it.path.stringValueOfPathName =~ ".+${jobGroupWithoutServersName}.+"}
        List<MockedGraphiteSocket.SentDate> allSentDatesWithServerName = sent.findAll {it.path.stringValueOfPathName =~ ".+${jobGroupWithServersName}.+" }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithExpectedPath = sent.findAll {
            it.path.stringValueOfPathName == ( pathPrefix + '.' + jobGroupWithServersName + '.daily.csi' )
        }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithExpectedTimeStamp = sent.findAll { dailyDateExpectedToBeSent.toDate() == it.timestamp }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithFirstValue = sent.findAll { it.value == (firstDailyValueToSend * 100) }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithSecondValue = sent.findAll { it.value == (secondDailyValueToSend * 100) }

        then: "only the expected SentDates should appear"
        allSentDatesWithoutServername.size() == 0
        allSentDatesWithServerName.size() == sent.size()
        sent.size() == sentInTotal
        allSentDatesWithExpectedPath.size() == sentInTotal
        allSentDatesWithExpectedTimeStamp.size() == sentInTotal
        allSentDatesWithFirstValue.size() == 1
        allSentDatesWithSecondValue.size() == 1
    }

    void "writing weekly PageCsiCsiAggregations to graphite"() {
        given:
        CsiAggregationInterval interval = new CsiAggregationInterval(name: 'weekly', intervalInMinutes: WEEKLY).save()

        CsiAggregation firstWpmv = getCsiAggregation(interval, AggregationType.PAGE, firstWeeklyValueToSend, weeklyDateExpectedToBeSent, '1,2,3')
        CsiAggregation secondWpmv = getCsiAggregation(interval, AggregationType.PAGE, secondWeeklyValueToSend, weeklyDateExpectedToBeSent, '4,5,6')

        //mocking
        mockEventCsiAggregationService([])
        mockPageCsiAggregationService([firstWpmv, secondWpmv], WEEKLY)
        mockJobGroupCsiAggregationService([], WEEKLY)

        when: "we report PageCSIValues of the last week"
        DateTime cronjobStartsAt = weeklyDateExpectedToBeSent.plusMinutes(20)
        serviceUnderTest.reportPageCSIValuesOfLastWeek(cronjobStartsAt)
        Integer sentInTotal = 2
        List<MockedGraphiteSocket.SentDate> sent = graphiteSocketUsedInTests.sentDates
        List<MockedGraphiteSocket.SentDate> allSentDatesWithoutServername = sent.findAll {it.path.stringValueOfPathName =~ ".+${jobGroupWithoutServersName}.+"}
        List<MockedGraphiteSocket.SentDate> allSentDatesWithServerName = sent.findAll {it.path.stringValueOfPathName =~ ".+${jobGroupWithServersName}.+" }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithExpectedPath = sent.findAll {
            it.path.stringValueOfPathName == ( pathPrefix + '.' + jobGroupWithServersName + '.weekly.' + pageName + '.csi' )
        }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithExpectedTimeStamp = sent.findAll { weeklyDateExpectedToBeSent.toDate() == it.timestamp }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithFirstValue = sent.findAll { it.value == (firstWeeklyValueToSend * 100) }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithSecondValue = sent.findAll { it.value == (secondWeeklyValueToSend * 100) }

        then: "only the expected SentDates should appear"
        allSentDatesWithoutServername.size() == 0
        allSentDatesWithServerName.size() == sent.size()
        sent.size() == sentInTotal
        allSentDatesWithExpectedPath.size() == sentInTotal
        allSentDatesWithExpectedTimeStamp.size() == sentInTotal
        allSentDatesWithFirstValue.size() == 1
        allSentDatesWithSecondValue.size() == 1
    }


    void "writing WeeklyShopCsiCsiAggregations to graphite"() {
        given:
        CsiAggregationInterval interval = new CsiAggregationInterval(name: 'weekly', intervalInMinutes:WEEKLY).save()

        CsiAggregation firstWsmv = getCsiAggregation(interval, AggregationType.JOB_GROUP, firstWeeklyValueToSend, weeklyDateExpectedToBeSent, '1,2,3')
        CsiAggregation secondWsmv = getCsiAggregation(interval, AggregationType.JOB_GROUP, secondWeeklyValueToSend, weeklyDateExpectedToBeSent, '4,5,6')

        //mocking
        mockEventCsiAggregationService([])
        mockPageCsiAggregationService([], WEEKLY)
        mockJobGroupCsiAggregationService([firstWsmv, secondWsmv], WEEKLY)

        when: "we report ShopCSIValues of last week"
        DateTime cronjobStartsAt = weeklyDateExpectedToBeSent.plusMinutes(20)
        serviceUnderTest.reportShopCSIValuesOfLastWeek(cronjobStartsAt)
        List<MockedGraphiteSocket.SentDate> sent = graphiteSocketUsedInTests.sentDates
        Integer sentInTotal = 2
        List<MockedGraphiteSocket.SentDate> allSentDatesWithoutServername = sent.findAll {it.path.stringValueOfPathName =~ ".+${jobGroupWithoutServersName}.+"}
        List<MockedGraphiteSocket.SentDate> allSentDatesWithServerName = sent.findAll {it.path.stringValueOfPathName =~ ".+${jobGroupWithServersName}.+" }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithExpectedPath = sent.findAll {
            it.path.stringValueOfPathName == ( pathPrefix + '.' + jobGroupWithServersName + '.weekly.csi')
        }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithExpectedTimeStamp = sent.findAll { weeklyDateExpectedToBeSent.toDate() == it.timestamp }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithFirstValue = sent.findAll { it.value == (firstWeeklyValueToSend * 100) }
        List<MockedGraphiteSocket.SentDate> allSentDatesWithSecondValue = sent.findAll { it.value == (secondWeeklyValueToSend * 100) }

        then: "only the expected SentDates should appear"
        allSentDatesWithoutServername.size() == 0
        allSentDatesWithServerName.size() == sent.size()
        sent.size() == sentInTotal
        allSentDatesWithExpectedPath.size() == sentInTotal
        allSentDatesWithExpectedTimeStamp.size() == sentInTotal
        allSentDatesWithFirstValue.size() == 1
        allSentDatesWithSecondValue.size() == 1
    }

    //mocks of inner services///////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void mockJobGroupDaoService() {
        def jobGroupService = Stub(JobGroupService) {
            findCSIGroups() >> {
                JobGroup jobGroupWithGraphiteServers = JobGroup.build(name: jobGroupWithServersName, graphiteServers: getGraphiteServers())
                JobGroup jobGroupWithoutGraphiteServers = JobGroup.build(name: jobGroupWithoutServersName, graphiteServers: [])
                return [jobGroupWithGraphiteServers,jobGroupWithoutGraphiteServers ] as Set
            }
        }
        serviceUnderTest.jobGroupService = jobGroupService
    }
    /**
     * Mocks {@linkplain GraphiteSocketProvider#getSocket}.
     * Field {@link #graphiteSocketUsedInTests} is returned and can be used to proof sent dates.
     */
    private void mockGraphiteSocketProvider() {
        def graphiteSocketProvider = Stub(DefaultGraphiteSocketProvider) {
            getSocket(_ as GraphiteServer) >> { GraphiteServer server ->
                graphiteSocketUsedInTests = new MockedGraphiteSocket()
                return graphiteSocketUsedInTests
            }
        }
        serviceUnderTest.graphiteSocketProvider = graphiteSocketProvider
    }
    /**
     //	 * Mocks {@linkplain EventCsiAggregationService#getOrCalculateHourylCsiAggregations}.
     */
    private void mockEventCsiAggregationService(Collection<CsiAggregation> toReturnFromGetHourlyCsiAggregations) {
        def eventCsiAggregationService = Stub(EventCsiAggregationService)
        eventCsiAggregationService.getHourlyCsiAggregations(_ as Date, _ as Date, _ as MvQueryParams) >> toReturnFromGetHourlyCsiAggregations
        serviceUnderTest.eventCsiAggregationService = eventCsiAggregationService
    }
    /**
     * Mocks {@linkplain PageCsiAggregationService#getOrCalculatePageCsiAggregations}.
     */
    private void mockPageCsiAggregationService(Collection<CsiAggregation> toReturnOnDemandForGetOrCalculateCsiAggregations, Integer expectedIntervalInMinutes) {
        def pageCsiAggregationService = Stub(PageCsiAggregationService)
        pageCsiAggregationService.getOrCalculatePageCsiAggregations(_ as Date, _ as Date, _ as CsiAggregationInterval, _ as List) >> {
            Date fromDate, Date toDate, CsiAggregationInterval interval, List<JobGroup> csiGroups ->
                interval.intervalInMinutes != expectedIntervalInMinutes?[]: toReturnOnDemandForGetOrCalculateCsiAggregations
        }
        serviceUnderTest.pageCsiAggregationService = pageCsiAggregationService
    }
    private void mockBatchActivityService(){
        serviceUnderTest.batchActivityService = Stub(BatchActivityService){
            getActiveBatchActivity(_ as Class, _ as Activity,_ as String, _ as int,_ as Boolean) >> {Class c, Activity activity, String name, int maxStages, boolean observe ->
                return new BatchActivityUpdaterDummy(name,c.name,activity, maxStages, 5000)
            }
        }
    }

    /**
     * Mocks {@linkplain JobGroupCsiAggregationService#getOrCalculateShopCsiAggregations}.
     */
    private void mockJobGroupCsiAggregationService(Collection<CsiAggregation> toReturnOnDemandForGetOrCalculateCsiAggregations, Integer expectedIntervalInMinutes) {
        def jobGroupCsiAggregationService = Stub(JobGroupCsiAggregationService)
        jobGroupCsiAggregationService.getOrCalculateShopCsiAggregations(_ as Date, _ as Date, _ as CsiAggregationInterval,_ as List) >> {
            Date fromDate, Date toDate, CsiAggregationInterval interval, List<JobGroup> csiGroups ->
                return interval.intervalInMinutes != expectedIntervalInMinutes? [] : toReturnOnDemandForGetOrCalculateCsiAggregations
        }
        serviceUnderTest.jobGroupCsiAggregationService = jobGroupCsiAggregationService
    }

    //test data common to all tests///////////////////////////////////////////////////////////////////////////////////////////////////////////

    static Collection<GraphiteServer> getGraphiteServers() {

        GraphitePathCsiData pathEvent = new GraphitePathCsiData(prefix: pathPrefix, aggregationType: AggregationType.MEASURED_EVENT)
        GraphitePathCsiData pathPage = new GraphitePathCsiData(prefix: pathPrefix, aggregationType: AggregationType.PAGE)
        GraphitePathCsiData pathShop = new GraphitePathCsiData(prefix: pathPrefix, aggregationType: AggregationType.JOB_GROUP)

        GraphiteServer serverWithPaths = GraphiteServer.build(graphitePathsCsiData: [pathEvent,pathPage,pathShop],
                reportCsiAggregationsToGraphiteServer: true, save: false)

        return [serverWithPaths]
    }

    static getCsiAggregation(CsiAggregationInterval interval, AggregationType aggregationType, Double value, DateTime valueForStated, String resultIds) {
        return CsiAggregation.build(
            save: false,
            started: valueForStated.toDate(),
            interval: interval,
            aggregationType: aggregationType,
            csByWptDocCompleteInPercent: value,
            underlyingEventResultsByWptDocComplete: resultIds,
            page: Page.build(save: false, name: pageName),
            jobGroup: JobGroup.build(save: false, name: jobGroupWithoutServersName),
            measuredEvent: MeasuredEvent.build(save: false, name: eventName),
            browser: Browser.build(save: false, name: browserName),
            location: Location.build(save: false, location: locationLocation)
        )
    }

}
