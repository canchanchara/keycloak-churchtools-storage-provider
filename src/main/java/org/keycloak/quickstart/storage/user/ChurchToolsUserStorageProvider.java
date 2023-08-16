package org.keycloak.quickstart.storage.user;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.OnUserCache;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.quickstart.storage.user.churchtools.model.ServerCredentials;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;

import java.net.CookieManager;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class ChurchToolsUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        CredentialInputUpdater,
        CredentialInputValidator,
        OnUserCache {
    private static final Logger logger = Logger.getLogger(ChurchToolsUserStorageProvider.class);
    public static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";

    protected ComponentModel model;
    protected KeycloakSession session;

    private ServerCredentials serverCredentials;

    ChurchToolsUserStorageProvider(KeycloakSession session, ComponentModel model, ServerCredentials serverCredentials) {
        this.session = session;
        this.model = model;
        this.serverCredentials=serverCredentials;
    }

    @Override
    public void preRemove(RealmModel realm) {

    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {

    }

    @Override
    public void close() {
    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {

        final String persistenceId = StorageId.externalId(id);

        logger.info("getUserById: " + persistenceId);

        CookieManager cookieManager = ChurchToolsApi.login(serverCredentials);
        UserEntity userEntity = ChurchToolsApi.getUserById(serverCredentials, cookieManager, persistenceId);

        if (userEntity == null) {
            logger.info("could not find user by id: " + id);
            return null;
        }

        return new UserAdapter(session, realm, model, userEntity);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        logger.info("getUserByUsername: " + username);

        CookieManager cookieManager = ChurchToolsApi.login(serverCredentials);
        UserEntity userEntity = ChurchToolsApi.getUserByEmailOrUsername(serverCredentials, cookieManager, username);

        if (userEntity == null) {
            logger.info("could not find user by username: " + username);
            return null;
        }

        return new UserAdapter(session, realm, model, userEntity);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        logger.info("getUserByEmail: " + email);

        CookieManager cookieManager = ChurchToolsApi.login(serverCredentials);
        UserEntity userEntity = ChurchToolsApi.getUserByEmailOrUsername(serverCredentials, cookieManager, email);

        if (userEntity == null) {
            logger.info("could not find user by email: " + email);
            return null;
        }

        return new UserAdapter(session, realm, model, userEntity);
    }


    @Override
    public void onCache(RealmModel realm, CachedUserModel user, UserModel delegate) {
        String password = ((UserAdapter) delegate).getPassword();
        if (password != null) {
            user.getCachedWith().put(PASSWORD_CACHE_KEY, password);
        }
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;
        UserCredentialModel cred = (UserCredentialModel) input;
        UserAdapter adapter = getUserAdapter(user);
        adapter.setPassword(cred.getValue());

        return true;
    }

    public UserAdapter getUserAdapter(UserModel user) {
        if (user instanceof CachedUserModel) {
            return (UserAdapter) ((CachedUserModel) user).getDelegateForUpdate();
        } else {
            return (UserAdapter) user;
        }
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
        if (!supportsCredentialType(credentialType)) return;

        getUserAdapter(user).setPassword(null);

    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        if (getUserAdapter(user).getPassword() != null) {
            Set<String> set = new HashSet<>();
            set.add(PasswordCredentialModel.TYPE);
            return set.stream();
        } else {
            return Stream.empty();
        }
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType) && getPassword(user) != null;
    }


    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;
        UserCredentialModel cred = (UserCredentialModel) input;
        return ChurchToolsApi.credentialsValid(serverCredentials, user.getEmail(), cred.getValue());
    }

    public String getPassword(UserModel user) {
        String password = null;
        if (user instanceof CachedUserModel) {
            password = (String) ((CachedUserModel) user).getCachedWith().get(PASSWORD_CACHE_KEY);
        } else if (user instanceof UserAdapter) {
            password = ((UserAdapter) user).getPassword();
        }
        return password;
    }

}
