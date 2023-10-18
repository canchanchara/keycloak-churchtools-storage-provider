package org.keycloak.quickstart.storage.user;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.quickstart.storage.user.churchtools.model.PersonDto;
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
        UserQueryProvider,
        CredentialInputValidator,
        CredentialInputUpdater {

    // This provider uses Keycloak's default cache for queried users. Credentials are not cached.
    // While that would improve performance, invalidation would be challenging to implement correctly
    // in order to prevent an attacker to use an outdated password that has just been changed.

    private static final Logger logger = Logger.getLogger(ChurchToolsUserStorageProvider.class);

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
    public void close() {
    }

    // UserLookupProvider
    @Override
    public UserModel getUserById(RealmModel realm, String id) {

        final String persistenceId = StorageId.externalId(id);

        CookieManager cookieManager = ChurchToolsApi.login(serverCredentials);
        PersonDto personDto = ChurchToolsApi.getUserById(serverCredentials, cookieManager, persistenceId);

        if (personDto == null) {
            logger.info("Could not find user with id: " + id);
            return null;
        }

        return new ChurchToolsUserAdapter(session, realm, model, personDto);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        return getUserByIdentifier(realm, username);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        return getUserByIdentifier(realm, email);
    }

    private UserModel getUserByIdentifier(RealmModel realm, String identifier) {

        CookieManager cookieManager = ChurchToolsApi.login(serverCredentials);
        PersonDto personDto = ChurchToolsApi.getUserByEmailOrUsername(serverCredentials, cookieManager, identifier);

        if (personDto == null) {
            logger.info("Could not find user with email or username: " + identifier);
            return null;
        }

        return new ChurchToolsUserAdapter(session, realm, model, personDto);
    }

    // UserCountMethodsProvider
    @Override
    public int getUsersCount(RealmModel realm) {
        CookieManager cookieManager = ChurchToolsApi.login(serverCredentials);
        return ChurchToolsApi.getPersonCount(serverCredentials, cookieManager);
    }

    // UserQueryMethodsProvider
    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        logger.info("searchForUserStream with Search Params: firstResult" + firstResult + " maxResults " + maxResults);

        String searchString = params.get(UserModel.SEARCH);

        // Suche nach "*" kann Church Tools nicht verstehen, daher Suche nach "" empty String
        if (searchString == null || searchString.equals("*"))
            searchString = "";
        else
            searchString = searchString.trim();

        CookieManager cookieManager = ChurchToolsApi.login(serverCredentials);
        List<PersonDto> persons = ChurchToolsApi.findPersons(serverCredentials, cookieManager, searchString, firstResult, maxResults);

        return persons.stream().map(p -> new ChurchToolsUserAdapter(session, realm, model, p));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        logger.info("getGroupMembersStream is not supported and returns an empty Stream");
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        logger.info("searchForUserByUserAttributeStream is not supported and returns an empty Stream");
        return Stream.empty();
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
