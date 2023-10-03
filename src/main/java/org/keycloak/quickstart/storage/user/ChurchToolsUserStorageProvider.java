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

public class ChurchToolsUserStorageProvider implements UserStorageProvider,
        UserLookupProvider,
        CredentialInputUpdater,
        CredentialInputValidator,
        OnUserCache,
        UserQueryProvider {
    private static final Logger logger = Logger.getLogger(ChurchToolsUserStorageProvider.class);
    public static final String PASSWORD_CACHE_KEY = UserAdapter.class.getName() + ".password";

    protected ComponentModel model;
    protected KeycloakSession session;

    private ServerCredentials serverCredentials;

    ChurchToolsUserStorageProvider(KeycloakSession session, ComponentModel model, ServerCredentials serverCredentials) {
        this.session = session;
        this.model = model;
        this.serverCredentials = serverCredentials;
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
    public int getUsersCount(RealmModel realm) {
        CookieManager cookieManager = ChurchToolsApi.login(serverCredentials);
        return ChurchToolsApi.getPersonCount(serverCredentials, cookieManager);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        logger.info("searchForUserStream by Searchterm firstResult"+firstResult+ " maxResults: "+maxResults+ " searchTerm:" +search);
        double pages = 1;
        if (firstResult > 1) {
            pages = firstResult.doubleValue() / maxResults.doubleValue();
        }

        CookieManager cookieManager = ChurchToolsApi.login(serverCredentials);
        List<UserEntity> persons = ChurchToolsApi.findPersons(serverCredentials, cookieManager, search, (int) pages, maxResults);

        return persons.stream().map(p -> mapUserEntity(p, realm)).toList().stream();
    }

    private UserModel mapUserEntity(UserEntity userEntity, RealmModel realm) {
        return new UserAdapter(session, realm, model, userEntity);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        logger.info("searchForUserStream with Search Params: firstResult"+firstResult+ " maxResults "+maxResults);

        // only support searching by username
        String usernameSearchString = params.get("keycloak.session.realm.users.query.search");
        if (usernameSearchString != null) {
            return searchForUserStream(realm, usernameSearchString, firstResult, maxResults);
        }

        // if we are not searching by username, return all users
        return searchForUserStream(realm, "", firstResult, maxResults);
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
        if (input.getType().equals(PasswordCredentialModel.TYPE))
            throw new ReadOnlyException("user is read only for this update");
        return false;
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
    }

    @Override
    public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
        return Stream.empty();
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
