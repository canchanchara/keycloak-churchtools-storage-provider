package org.keycloak.quickstart.storage.user;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.quickstart.storage.user.churchtools.model.ServerCredentials;
import org.keycloak.storage.UserStorageProviderFactory;

public class ChurchToolsUserStorageProviderFactory implements UserStorageProviderFactory<ChurchToolsUserStorageProvider> {
    public static final String PROVIDER_ID = "churchtools-user-storage";

    private ServerCredentials serverCredentials;

    private static final Logger logger = Logger.getLogger(ChurchToolsUserStorageProviderFactory.class);

    @Override
    public ChurchToolsUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new ChurchToolsUserStorageProvider(session, model, serverCredentials);
    }


    @Override
    public void init(Config.Scope configScope) {
        this.serverCredentials = new ServerCredentials();
        serverCredentials.setInstance(configScope.get("instance"));
        serverCredentials.setUsername(configScope.get("username"));
        serverCredentials.setPassword(configScope.get("password"));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Church Tools User Storage Provider";
    }

    @Override
    public void close() {
        logger.info("<<<<<< Closing factory");

    }
}
