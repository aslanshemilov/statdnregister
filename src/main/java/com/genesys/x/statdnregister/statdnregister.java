package com.genesys.x.statdnregister;

import com.genesys.x.statdnregister.interfaces.IConfigAppProvider;
import com.genesys.x.statdnregister.interfaces.IConfigDNProvider;
import com.genesys.x.statdnregister.interfaces.IConfigSwitchProvider;
import com.genesys.x.statdnregister.interfaces.IStatDNConfiguration;
import com.genesyslab.platform.applicationblocks.com.objects.*;
import com.genesyslab.platform.applicationblocks.com.queries.CfgSwitchQuery;
import com.genesyslab.platform.commons.collections.Pair;
import com.genesyslab.platform.commons.protocol.ProtocolException;
import com.genesyslab.platform.configuration.protocol.types.CfgAppComponentType;
import com.genesyslab.platform.configuration.protocol.types.CfgAppType;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.io.IOException;

public class statdnregister {
	public static void main(String[] args) throws InterruptedException, ProtocolException {
        Injector injector = Guice.createInjector(new StatModule());

        IStatDNConfiguration config = injector.getInstance(IStatDNConfiguration.class);

        System.out.println(config.getConfigServerUrl());

        CfgConnection cfgserver = injector.getInstance(CfgConnection.class);

        cfgserver.open(false);
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
                    for (Object o : server.getAppPrototype().getUserProperties()) {
                        Pair p = (Pair) o;
                        System.out.println(p.getStringKey());
                        System.out.println(p.getStringValue());
                    }
                    for (Object o : server.getFlexibleProperties()){
                        Pair p = (Pair)o;
                        System.out.println(p.getStringKey());
                        System.out.println(p.getStringValue());
                    }

                    CfgSwitchQuery q = new CfgSwitchQuery();
                    q.setTserverDbid(server.getDBID());
                    CfgSwitch s = switchProvider.getSwitch(q);
                    System.out.println(s.getName());
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

    }
}