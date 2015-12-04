package com.genesys.x.statdnregister;

import com.genesyslab.platform.applicationblocks.com.objects.CfgConnInfo;
import com.genesyslab.platform.commons.protocol.ChannelClosedEvent;
import com.genesyslab.platform.commons.protocol.ChannelErrorEvent;
import com.genesyslab.platform.commons.protocol.ChannelListener;
import com.google.inject.Inject;

import java.util.EventObject;

/**
 * Created by dburdick on 12/3/2015.
 */
public class StatCfgListener implements ChannelListener {

    @Override
    public void onChannelOpened(EventObject eventObject) {

    }

    @Override
    public void onChannelClosed(ChannelClosedEvent channelClosedEvent) {

    }

    @Override
    public void onChannelError(ChannelErrorEvent channelErrorEvent) {

    }
}
