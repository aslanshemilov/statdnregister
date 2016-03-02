package com.genesys.x.statdnregister;

import com.genesys.x.statdnregister.interfaces.IConfigAppProvider;
import com.genesys.x.statdnregister.interfaces.IStatDNConfiguration;
import com.genesyslab.platform.applicationblocks.com.IConfService;
import com.genesyslab.platform.applicationblocks.com.objects.CfgApplication;
import com.genesyslab.platform.applicationblocks.com.objects.CfgPortInfo;
import com.genesyslab.platform.commons.protocol.*;
import com.genesyslab.platform.reporting.protocol.StatServerProtocol;
import com.genesyslab.platform.reporting.protocol.statserver.*;
import com.genesyslab.platform.reporting.protocol.statserver.events.EventInfo;
import com.genesyslab.platform.reporting.protocol.statserver.events.EventStatisticClosed;
import com.genesyslab.platform.reporting.protocol.statserver.events.EventStatisticOpened;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestCloseStatistic;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestOpenStatistic;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestOpenStatisticEx;
import com.genesyslab.platform.reporting.protocol.statserver.requests.RequestPeekStatistic;
import com.google.inject.Inject;

import java.util.*;
import java.util.concurrent.Executors;

/**
 * Created by dburdick on 12/3/2015.
 */
public class StatConnection implements ChannelListener, MessageHandler {
    @Inject
    private IConfigAppProvider appProvider;

    @Inject
    private IStatDNConfiguration config;

    @Inject
    ChannelListener channelListener;

    private StatServerProtocol statServer;

    public StatConnection() {
        statServer = new StatServerProtocol();
//        statServer.addChannelListener(this);
        statServer.addChannelListener(channelListener);
        statServer.setMessageHandler(this);
        System.out.println("Creating Stat Connection");
    }

    public void open(){
        /*new Thread(new Runnable() {
            @Override
            public void run() {*/
        try {
            System.out.println("Running");
            CfgApplication app = appProvider.getApplication(config.getStatServerName());
            int port = 0;

            if (app.getPortInfos() != null && app.getPortInfos().size() > 0) {
                for (CfgPortInfo pi : app.getPortInfos()) {
                    System.out.format("ID: %s Port: %s\n", pi.getId(), pi.getPort());
                    if (pi.getId() == "" || pi.getId() == "default") {
                        System.out.format("getPort is %s\n");
                        port = Integer.valueOf(pi.getPort());
                        break;
                    }
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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
            /*}
        }).start();*/
    }

    public void close() {
        if (statServer.getState() != ChannelState.Closed) {
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
        System.out.println("stat opened");
    }

    @Override
    public void onChannelClosed(ChannelClosedEvent channelClosedEvent) {
        System.out.println("stat closed");
    }

    @Override
    public void onChannelError(ChannelErrorEvent channelErrorEvent) {
        System.out.println("stat error");
    }

    @Override
    public void onMessage(Message message) {
        System.out.println(message.toString());
    }

    private HashMap<Integer, RequestOpenStatistic> statIDList = new HashMap<Integer, RequestOpenStatistic>();

    public EventInfo peekStatistic(int id) {

        if (!statIDList.containsKey(id)) {
            return null;
        }

        System.out.format("Requesting statistic %d\n", id);
        RequestPeekStatistic peek = RequestPeekStatistic.create(id);
        try {
            Message msg = statServer.request(peek, 5000);
            if (msg instanceof EventInfo) {
                return (EventInfo) msg;
            } else {
                if (msg == null){
                    System.out.format("RequestPeekStatistic %d is null\n", id);
                }else {
                System.out.format("Invalid result for %d. %s", id, msg.toString());
            }
            }

        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        return null;
    }

    public EventStatisticOpened openStatistic(String tenant, String objectId, StatisticObjectType objectType, String statType) {
        RequestOpenStatistic open;
        StatisticObject statObject = StatisticObject.create();
        statObject.setObjectId(objectId);
        statObject.setObjectType(objectType);
        statObject.setTenantName(tenant);

        StatisticMetric metric = StatisticMetric.create();
        metric.setStatisticType(statType);

        Notification notification = Notification.create();
        notification.setMode(NotificationMode.NoNotification);

        open = RequestOpenStatistic.create(statObject, metric, notification);
        try {
            Message msg = statServer.request(open);
            if (msg instanceof EventStatisticOpened) {
                statIDList.put(((EventStatisticOpened) msg).getReferenceId(), open);
                return (EventStatisticOpened) msg;
            } else {
                System.err.println(msg.toString());
            }
        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void closeStatistic(int id) {
        if (statIDList.containsKey(id)) {
            statIDList.remove(id);
            RequestCloseStatistic close = RequestCloseStatistic.create(id);
            try {
                Message msg = statServer.request(close);
                if (msg instanceof EventStatisticClosed) {

                } else {
                    System.err.println(msg.toString());
                }
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
        }
    }
}
