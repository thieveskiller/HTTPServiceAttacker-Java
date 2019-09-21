package com.ishland.app.HTTPServiceAttacker.configuration;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class LoggingEventHandler extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
	if (event.getLoggerName().contains("AsyncRetryExec"))
	    return FilterReply.DENY;
	return FilterReply.ACCEPT;
    }

}
