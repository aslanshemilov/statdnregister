package com.genesys.x.statdnregister;

import com.genesys.x.statdnregister.interfaces.IConfigAppProvider;
import com.genesys.x.statdnregister.interfaces.IConfigDNProvider;
import com.genesys.x.statdnregister.interfaces.IConfigSwitchProvider;
import com.genesys.x.statdnregister.interfaces.IStatDNConfiguration;
import com.genesyslab.platform.applicationblocks.com.objects.*;
import com.genesyslab.platform.applicationblocks.com.queries.CfgDNQuery;
import com.genesyslab.platform.applicationblocks.com.queries.CfgSwitchQuery;
import com.genesyslab.platform.commons.protocol.ProtocolException;
import com.genesyslab.platform.configuration.protocol.types.CfgAppType;
import com.genesyslab.platform.configuration.protocol.types.CfgDNType;
import com.genesyslab.platform.reporting.protocol.statserver.StatisticObjectType;
import com.genesyslab.platform.reporting.protocol.statserver.events.EventInfo;
import com.genesyslab.platform.reporting.protocol.statserver.events.EventStatisticOpened;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.io.IOException;
import java.util.Collection;

public class statdnregister {
	public static void main(String[] args) throws InterruptedException, ProtocolException {
        Injector injector = Guice.createInjector(new StatModule());

        IStatDNConfiguration config = injector.getInstance(IStatDNConfiguration.class);

        CfgConnection cfgserver = injector.getInstance(CfgConnection.class);
        cfgserver.open(false);

        StatConnection statServer = injector.getInstance(StatConnection.class);
        statServer.open();

        ConfigObjectProvider searcher = injector.getInstance(ConfigObjectProvider.class);

        IConfigAppProvider appProvider = injector.getInstance(IConfigAppProvider.class);
        if (appProvider == null){
            System.out.println("appProvider is null");
        }
        IConfigSwitchProvider switchProvider = injector.getInstance(IConfigSwitchProvider.class);
        IConfigDNProvider dnProvider = injector.getInstance(IConfigDNProvider.class);

        try {
            CfgApplication app = appProvider.getApplication(config.getStatServerName());
            for (CfgConnInfo con: app.getAppServers()) {
                CfgApplication server = con.getAppServer();
                if (server.getType() == CfgAppType.CFGTServer) {
                    System.out.println(server.getName());
                    CfgSwitchQuery q = new CfgSwitchQuery();
                    q.setTserverDbid(server.getDBID());
                    CfgSwitch s = switchProvider.getSwitch(q);
                    System.out.println(s.getName());

                    CfgDNQuery dnq = new CfgDNQuery();
                    System.out.println(s.getDBID());
                    dnq.setSwitchDbid(s.getDBID());
                    Collection<CfgDN> dnlist = dnProvider.getDNs(dnq);
                    System.out.println(dnlist.size());
                    for (CfgDN d: dnlist){
                        EventStatisticOpened opened = null;
                        String name = String.format("%s@%s", d.getNumber(), s.getName());
                        String tenant = d.getTenant().getName();
                        if (d.getType() == CfgDNType.CFGExtension){
                            opened = statServer.openStatistic(tenant, name,
                                    StatisticObjectType.RegularDN, config.getExtensionStatistic());
                        } else if (d.getType() == CfgDNType.CFGVirtACDQueue){
                            opened = statServer.openStatistic(tenant, name,
                                    StatisticObjectType.Queue, config.getVirtualQueueStatistic());
                        }else if (d.getType() == CfgDNType.CFGRoutingQueue){
                            opened = statServer.openStatistic(tenant, name,
                                    StatisticObjectType.RoutePoint, config.getVirtualQueueStatistic());
                        }

                        if (opened != null){
                            System.out.format("Registered %s with id %d\n", name, opened.getReferenceId());
                            EventInfo eventInfo = statServer.peekStatistic(opened.getReferenceId());
                            if (eventInfo != null){
                                System.out.format("Peek Value %s\n", eventInfo.getStringValue());
                            }
                        }
                    }
                }
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        cfgserver.close();
        statServer.close();

    }
}