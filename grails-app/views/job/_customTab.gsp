<div id="customMetrics" class="form-group ${hasErrors(bean: job, field: customMetrics, 'has-error')}">
    <label for="inputField-customMetrics" class="col-md-3 control-label">
        <g:message code="job.customMetrics.label" default="customMetrics" />
    </label>
    <div class="col-md-7">
        <textarea class="form-control" name="customMetrics" rows="3" id="inputField-customMetrics">${job?.customMetrics?.trim()}</textarea>
        <p style="margin-top: 5px"><g:message code="job.customMetrics.info" default="info"/></p>
    </div>
</div>