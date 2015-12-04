package com.genesys.x.statdnregister;

import com.genesys.x.statdnregister.interfaces.IConfigAppProvider;
import com.genesys.x.statdnregister.interfaces.IStatDNConfiguration;
import com.genesyslab.platform.applicationblocks.com.IConfService;
import com.genesyslab.platform.applicationblocks.com.objects.CfgApplication;
import com.genesyslab.platform.applicationblocks.com.objects.CfgPortInfo;
import com.genesyslab.platform.commons.protocol.*;
import com.genesyslab.platform.reporting.protocol.StatServerProtocol;
import com.google.inject.Inject;

import java.util.EventObject;
import java.util.concurrent.Executors;

/**
 * Created by dburdick on 12/3/2015.
 */
public class StatConnection implements ChannelListener, MessageHandler {
    @Inject
    private IConfigAppProvider appProvider;

    @Inject
    private IStatDNConfiguration config;

    @Inject ChannelListener channelListener;

    private StatServerProtocol statServer;
    public StatConnection() {
        statServer = new StatServerProtocol();
        statServer.addChannelListener(this);
        statServer.addChannelListener(channelListener);
        statServer.setMessageHandler(this);
System.out.println("Creating Stat Connection");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Running");
                    CfgApplication app = appProvider.getApplication(config.getStatServerName());
                    int port = 0;

                    for (CfgPortInfo pi : app.getPortInfos()) {
                        System.out.format("ID: %s Port: %s\n", pi.getId(), pi.getPort());
                        if (pi.getId() == "" || pi.getId() == "default") {
                            System.out.format("getPort is %s\n");
                            port = Integer.valueOf(pi.getPort());
                            break;
                        }
                    }
                    if (port == 0) {
                        System.out.println(app.getServerInfo().getPort());
                        port = Integer.valueOf(app.getServerInfo().getPort());
                    }
                    Endpoint endpoint = new Endpoint(app.getServerInfo().getHost().getName(),
                            port);

                    statServer.setEndpoint(endpoint);
                    try {
                        statServer.open();
                    } catch (ProtocolException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    public void close(){
        if (statServer.getState() != ChannelState.Closed){
            try {
                statServer.close();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onChannelOpened(EventObject eventObject) {

    }

    @Override
    public void onChannelClosed(ChannelClosedEvent channelClosedEvent) {

    }

    @Override
    public void onChannelError(ChannelErrorEvent channelErrorEvent) {

    }

    @Override
    public void onMessage(Message message) {

    }
}
