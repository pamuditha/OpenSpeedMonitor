<li class="dropdown">
    <a class="dropdown-toggle" data-toggle="dropdown" href="#"><g:message code="navbar.browse" default="Browse" locale="${lang}"/> <b class="caret"></b></a>
    <ul class="dropdown-menu">

        %{--measurement --------------------------------------------------------}%

        <li class="controller">
            <g:link controller="job" action="index"><i class="fa fa-calendar"></i> <g:message
                    code="de.iteratec.isj.jobs" default="Jobs"/></g:link>
        </li>
        <li class="controller">
            <g:link controller="script" action="list"><i class="fa fa-align-left"></i> <g:message
                    code="de.iteratec.iss.scripts" default="Skripte"/></g:link>
        </li>
        <li class="controller">
            <g:link controller="queueStatus" action="list"><i class="fa fa-inbox"></i> <g:message
                    code="queue.status.label"/></g:link>
        </li>
        <li class="controller">
            <g:link controller="jobSchedule" action="schedules"><i class="fa fa-clock-o"></i> <g:message
                    code="job.Schedule.label"/></g:link>
        </li>
        <li class="controller">
            <g:link controller="connectivityProfile" action="list"><i class="fa fa-globe"></i> <g:message
                    code="connectivityProfile.label.plural"/></g:link>
        </li>
        <li class="divider"></li>

        %{--results --------------------------------------------------------}%

        <li class="controller">
            <g:link controller="eventResultDashboard" action="showAll"><i class="fa fa-signal"></i>
                <g:message code="eventResultDashboard.label" default="Result Dashboard"/></g:link>
        </li>
        <li class="controller">
            <g:link controller="tabularResultPresentation" action="listResults"><i
                    class="fa fa-th-list"></i> <g:message code="de.iteratec.result.title"
                                                          default="Einzelergebnisse"/></g:link>
        </li>
        <li class="divider"></li>

        %{--csi --------------------------------------------------------}%

        <li class="controller">
            <g:link controller="csiDashboard" action="showAll"><i class="fa fa-signal"></i> <g:message
                    code="csiDashboard.label" default="CSI Dashboard"/></g:link>
        </li>
        <li class="controller">
            <g:link controller="csiConfiguration" action="configurations"><i class="fa fa-gears"></i> <g:message
                    code="de.iteratec.osm.csi.configuration.label" default="CSI Configuration"/></g:link>
        </li>

    </ul>
</li>