package de.canchanchara.keycloak.storage;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

public class ChurchToolsUserStorageProviderFactory implements UserStorageProviderFactory<ChurchToolsUserStorageProvider> {
    public static final String PROVIDER_ID = "churchtools-user-storage";
    private static final Logger logger = Logger.getLogger(ChurchToolsUserStorageProviderFactory.class);

    // Using a single API instance in the factory enables HTTP connection pooling for better performance
    private ChurchToolsApi churchTools;


    @Override
    public ChurchToolsUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new ChurchToolsUserStorageProvider(session, model, churchTools);
    }

    @Override
    public void init(Config.Scope configScope) {
        String host = configScope.get("host");
        String loginToken = configScope.get("login-token");
        if (host == null || host.isEmpty() || loginToken == null || loginToken.isEmpty())
            logger.warn("ChurchTools configuration is incomplete. User Federation with ChurchTools will not work.");

        churchTools = ChurchToolsApi.createWithLoginToken(host, loginToken);
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
