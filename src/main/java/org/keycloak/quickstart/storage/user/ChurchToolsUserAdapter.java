package org.keycloak.quickstart.storage.user;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.quickstart.storage.user.churchtools.model.PersonDto;
import org.keycloak.storage.StorageId;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A read-only user adapter for data from ChurchTools.
 */
public class ChurchToolsUserAdapter extends AbstractAttributeUserAdapter {
    protected PersonDto person;

    public ChurchToolsUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, PersonDto person) {
        super(session, realm, model);
        this.person = person;
        storageId = new StorageId(model.getProviderId(), person.getId());
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return Map.of(
                UserModel.FIRST_NAME, List.of(person.getFirstName()),
                UserModel.LAST_NAME, List.of(person.getLastName()),
                UserModel.EMAIL, List.of(person.getEmail()),
                UserModel.EMAIL_VERIFIED, List.of(Boolean.toString(true)),
                UserModel.USERNAME, List.of(person.getCmsUserId()),
                CREATED_TIMESTAMP, List.of(Long.toString(
                        Instant.parse(person.getMeta().getCreatedDate()).toEpochMilli()
                ))
        );
    }
}
