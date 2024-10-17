package de.canchanchara.keycloak.storage;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import de.canchanchara.keycloak.storage.churchtools.model.PersonDto;
import org.keycloak.storage.StorageId;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A read-only user adapter for data from ChurchTools.
 */
public class ChurchToolsUserAdapter extends AbstractAttributeUserAdapter {
    private final Map<String, List<String>> attributes;

    public ChurchToolsUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, PersonDto person) {
        super(session, realm, model);
        storageId = new StorageId(model.getId(), person.getId());
        if (person.getImageUrl() == null) {
            attributes = Map.of(
                    UserModel.FIRST_NAME, List.of(person.getFirstName()),
                    UserModel.LAST_NAME, List.of(person.getLastName()),
                    UserModel.EMAIL, List.of(person.getEmail()),
                    UserModel.EMAIL_VERIFIED, List.of(Boolean.toString(true)),
                    UserModel.USERNAME, List.of(person.getCmsUserId()),
                    UserModel.ENABLED, List.of(Boolean.toString(true)),
                    CREATED_TIMESTAMP, List.of(Long.toString(
                            Instant.parse(person.getMeta().getCreatedDate()).toEpochMilli()
                    ))
            );
        } else {
            attributes = Map.of(
                    UserModel.FIRST_NAME, List.of(person.getFirstName()),
                    UserModel.LAST_NAME, List.of(person.getLastName()),
                    UserModel.EMAIL, List.of(person.getEmail()),
                    UserModel.EMAIL_VERIFIED, List.of(Boolean.toString(true)),
                    UserModel.USERNAME, List.of(person.getCmsUserId()),
                    UserModel.ENABLED, List.of(Boolean.toString(true)),
                    PICTURE, List.of(person.getImageUrl()),
                    CREATED_TIMESTAMP, List.of(Long.toString(
                            Instant.parse(person.getMeta().getCreatedDate()).toEpochMilli()
                    ))
            );
        }
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return attributes;
    }
}
