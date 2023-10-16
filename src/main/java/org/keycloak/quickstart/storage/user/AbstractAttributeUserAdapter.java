package org.keycloak.quickstart.storage.user;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.LegacyUserCredentialManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.storage.adapter.AbstractUserAdapter;

import java.util.List;
import java.util.stream.Stream;

/**
 * This adapter extends AbstractUserAdapter because of its read-only nature and provides default getters like
 * AbstractUserAdapterFederatedStorage which is intended for bidirectional sync implementing UserFederatedStorageProvider.
 */
public abstract class AbstractAttributeUserAdapter extends AbstractUserAdapter {
    public static final String CREATED_TIMESTAMP = "createdTimestamp";

    public AbstractAttributeUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel) {
        super(session, realm, storageProviderModel);
    }

    @Override
    public SubjectCredentialManager credentialManager() {
        return new LegacyUserCredentialManager(session, realm, this);
    }

    @Override
    public String getFirstAttribute(String name) {
        List<String> values = getAttributes().get(name);
        if (values != null && !values.isEmpty())
            return values.get(0);
        else
            return null;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        List<String> values = getAttributes().get(name);
        if (values == null)
            return Stream.empty();
        else
            return values.stream();
    }

    @Override
    public String getFirstName() {
        return getFirstAttribute(UserModel.FIRST_NAME);
    }

    @Override
    public String getLastName() {
        return getFirstAttribute(UserModel.LAST_NAME);
    }

    @Override
    public String getEmail() {
        return getFirstAttribute(UserModel.EMAIL);
    }

    @Override
    public boolean isEmailVerified() {
        return Boolean.parseBoolean(getFirstAttribute(UserModel.EMAIL_VERIFIED));
    }

    @Override
    public String getUsername() {
        return getFirstAttribute(UserModel.USERNAME);
    }

    @Override
    public Long getCreatedTimestamp() {
        try {
            return Long.parseLong(getFirstAttribute(CREATED_TIMESTAMP));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

