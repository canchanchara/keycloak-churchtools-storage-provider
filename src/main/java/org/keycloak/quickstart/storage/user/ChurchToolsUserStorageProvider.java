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
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

import java.net.CookieManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ChurchToolsUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater,
        OnUserCache {
    private static final Logger logger = Logger.getLogger(ChurchToolsUserStorageProvider.class);
    public static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";

    protected ComponentModel model;
    protected KeycloakSession session;

    private final ServerCredentials serverCredentials;

    ChurchToolsUserStorageProvider(KeycloakSession session, ComponentModel model, ServerCredentials serverCredentials) {
        this.session = session;
        this.model = model;
        this.serverCredentials = serverCredentials;
    }

    // UserStorageProvider
    // No cleanup required because this provider does not use a persistent storage
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

    // UserLookupProvider
    @Override
    public int getUsersCount(RealmModel realm) {
        CookieManager cookieManager = ChurchToolsApi.login(serverCredentials);
        return ChurchToolsApi.getPersonCount(serverCredentials, cookieManager);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        logger.info("searchForUserStream by Searchterm firstResult" + firstResult + " maxResults: " + maxResults + " searchTerm:" + search);

        CookieManager cookieManager = ChurchToolsApi.login(serverCredentials);
        List<UserEntity> persons = ChurchToolsApi.findPersons(serverCredentials, cookieManager, search, firstResult, maxResults);

        return persons.stream().map(p -> mapUserEntity(p, realm)).toList().stream();
    }

    private UserModel mapUserEntity(UserEntity userEntity, RealmModel realm) {
        return new UserAdapter(session, realm, model, userEntity);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        logger.info("searchForUserStream with Search Params: firstResult" + firstResult + " maxResults " + maxResults);

        String searchString = params.get("keycloak.session.realm.users.query.search");

        if (searchString == null) {
            return searchForUserStream(realm, "", firstResult, maxResults);
        }

        searchString = searchString.trim();

        // Suche nach "*" kann Church Tools nicht verstehen, daher Suche nach "" empty String
        if (searchString.equals("*")) {
            return searchForUserStream(realm, "", firstResult, maxResults);
        }

        return searchForUserStream(realm, searchString, firstResult, maxResults);
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        logger.info("getGroupMembersStream");
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        logger.info("searchForUserByUserAttributeStream");
        return Stream.empty();
    }


    @Override
    public UserModel getUserById(RealmModel realm, String id) {

        final String persistenceId = StorageId.externalId(id);

        logger.info("getUserById: " + persistenceId);

        CookieManager cookieManager = ChurchToolsApi.login(serverCredentials);
        UserEntity userEntity = ChurchToolsApi.getUserById(serverCredentials, cookieManager, persistenceId);

        if (userEntity == null || userEntity.getId() == null || userEntity.getId().isEmpty()) {
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

        if (userEntity == null || userEntity.getId() == null || userEntity.getId().isEmpty()) {
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

        if (userEntity == null || userEntity.getId() == null || userEntity.getId().isEmpty()) {
            logger.info("could not find user by email: " + email);
            return null;
        }

        return new UserAdapter(session, realm, model, userEntity);
    }

    // CredentialInputValidator
    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        // All users for which a UserModel can be created must have username and password set and are therefore valid
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType())) return false;
        return ChurchToolsApi.credentialsValid(serverCredentials, user.getEmail(), input.getChallengeResponse());
    }

    // CredentialInputUpdater
    // This interface must be implemented to prevent password changes to Keycloak local storage
    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
        if (supportsCredentialType(input.getType()))
            throw new ReadOnlyException("User is read-only for this update");

        return false;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
    }
}
