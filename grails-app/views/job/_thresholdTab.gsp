<%@ page import="de.iteratec.osm.result.Threshold" %>
<%@ page import="de.iteratec.osm.measurement.schedule.JobGroup" %>
<%@ page import="de.iteratec.osm.result.Measurand" %>

<div id="threshold">
    <div>
        <div id="thresholdList">
            <div>
                <div v-for="thresholdItem in thresholds"><label class="measuredEventLabel">{{ thresholdItem.measuredEvent.name }}</label>
                    <div v-for="threshold in thresholdItem.thresholdList">
                    <div class="thresholdMeasurand"><label>{{ threshold.threshold.measurand.name }}</label>
                            <div class="thresholdBoundaries form-inline">
                                <label class="text-success">
                                    Good
                                </label>
                                <label>
                                    <
                                </label>
                                <input v-if="threshold.edit" id="lowerBoundaryEdit" class="form-control" type="number" min="1"
                                       name="lowerBoundary" cols="30"
                                       rows="5"
                                       maxlength="150"
                                       v-model="threshold.threshold.lowerBoundary"/>
                                <input v-else id="lowerBoundaryShow" class="form-control float-right" type="number" min="1"
                                       name="lowerBoundary" cols="30"
                                       rows="5"
                                       maxlength="150"
                                       readonly
                                       v-model="threshold.threshold.lowerBoundary"/>
                                <label>
                                    ms <
                                </label>
                                <label class="text-warning">
                                    OK
                                </label>
                                <label>
                                    <
                                </label>
                                <input v-if="threshold.edit" id="upperBoundaryEdit" class="form-control" type="number" min="1"
                                       name="upperBoundary" cols="40"
                                       rows="5"
                                       maxlength="150"
                                       v-model="threshold.threshold.upperBoundary"/>
                                <input v-else id="upperBoundaryShow" class="form-control float-right" type="number" min="1"
                                       name="upperBoundary" cols="40"
                                       rows="5"
                                       maxlength="150"
                                       readonly
                                       v-model="threshold.threshold.upperBoundary"/>
                                <label>
                                    ms <
                                </label>
                                <label class="text-danger">
                                    Bad
                                </label>
                                <span v-if="threshold.edit">
                                <button type="button"
                                        class="thresholdButton margins btn btn-warning btn-xs"
                                        @click="updateThreshold(threshold, '${g.createLink([controller: 'threshold', action: 'updateAsync'])}')">submit</button>
                                <button type="button"
                                        class="thresholdButton btn btn-danger btn-xs"
                                        @click="changeEditMode(threshold, false)">discard</button>
                                </span>
                                <span v-else>
                                    <button type="button"
                                            class="thresholdButton margins btn btn-warning btn-xs"
                                            @click="changeEditMode(threshold, true)">edit</button>
                                    <button type="button"
                                            class="thresholdButton btn btn-danger btn-xs"
                                            @click="deleteThreshold(threshold, '${g.createLink([controller: 'threshold', action: 'deleteAsync'])}')">delete</button>
                                </span>
                            </div>

                        </div>
                    </div>
                    <br>
                </div>
            </div>
        </div>
    </div>

    <br> <br>

    <div><label>New threshold:</label> <br>
        <div>
        <select id="measuredEvent" name="measuredEvent" class="form-control thresholdSelects"
                v-model="newThreshold.measuredEvent">
            <option v-for="measuredEvent in measuredEvents" :value="measuredEvent">{{measuredEvent.name}}</option>
        </select>
        </div>
        <select class="thresholdMeasurand form-control thresholdSelects" id="measurand" name="measurand" v-model="newThreshold.measurand">
            <option v-for="measurand in measurands" :value="measurand">{{measurand.name}}</option>
        </select>

        <div class="thresholdBoundaries form-inline thresholdSelects">
            <label class="text-success">
                Good
            </label>
            <label>
                <
            </label>
            <input id="lowerBoundary" class="form-control" type="number" min="1"
                   name="lowerBoundary" cols="30"
                   rows="5"
                   maxlength="150"
                   v-model="newThreshold.lowerBoundary"/>
            <label>
                ms <
            </label>
            <label class="text-warning">
                OK
            </label>
            <label>
                <
            </label>
            <input id="upperBoundary" class="form-control" type="number" min="1"
                   name="upperBoundary" cols="40"
                   rows="5"
                   maxlength="150"
                   v-model="newThreshold.upperBoundary"/>
            <label>
                ms <
            </label>
            <label class="text-danger">
                Bad
            </label>
            <button type="button" class="margins btn btn-default"
                    @click="addThreshold('${job}', '${g.createLink([controller: 'threshold', action: 'createAsync'])}')">
                <i class="fa text-success fa-lg fa-plus"></i>
            </button>
        </div>
    </div>


    <div class="ciButton">
        <button class="btn btn-default" type="button" id="copyToClipboard">
            <g:message code="job.threshold.copyToClipboard" default="Copy To Clipboard"/>
        </button>
    </div>
</div>


<asset:script type="text/javascript">
    OpenSpeedMonitor.postLoader.loadJavascript('<g:assetPath src="job/thresholdForJob.js"/>');
    $(window).load(function() {
      OpenSpeedMonitor.thresholdforJobs.initVue({jobId: "${job?.id}", scriptId: "${job?.script?.id}"});
    });
</asset:script>