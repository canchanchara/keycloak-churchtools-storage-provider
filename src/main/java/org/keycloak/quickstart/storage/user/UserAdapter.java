package org.keycloak.quickstart.storage.user;

import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class UserAdapter extends AbstractUserAdapterFederatedStorage {
    private static final Logger logger = Logger.getLogger(UserAdapter.class);
    protected UserEntity entity;
    protected String keycloakId;

    public UserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, UserEntity entity) {
        super(session, realm, model);
        this.entity = entity;
        keycloakId = StorageId.keycloakId(model, entity.getId());
    }

    public String getPassword() {
        return entity.getPassword();
    }

    public void setPassword(String password) {
        entity.setPassword(password);
    }

    @Override
    public String getUsername() {
        return entity.getUsername();
    }

    @Override
    public void setUsername(String username) {
        entity.setUsername(username);
    }

    @Override
    public void setEmail(String email) {
        entity.setEmail(email);
    }

    @Override
    public String getEmail() {
        return entity.getEmail();
    }


    @Override
    public void setLastName(String lastName) {
        entity.setLastname(lastName);
    }


    @Override
    public void setFirstName(String firstName) {
        entity.setFirstname(firstName);
    }


    @Override
    public String getFirstName() {
        return entity.getFirstname();
    }

    @Override
    public String getLastName() {
        return entity.getLastname();
    }

    @Override
    public String getId() {
        return keycloakId;
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        logger.debug("setSingleAttribute: " + name);

        if (name.equals("phone")) {
            entity.setPhone(value);
        } else {
            super.setSingleAttribute(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        logger.debug("removeAttribute: " + name);

        if (name.equals("phone")) {
            entity.setPhone(null);
        } else {
            super.removeAttribute(name);
        }
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        logger.debug("setAttribute: " + name);

        if (name.equals("phone")) {
            entity.setPhone(values.get(0));
        } else {
            super.setAttribute(name, values);
        }
    }

    @Override
    public String getFirstAttribute(String name) {
        logger.debug("getFirstAttribute: " + name);

        switch (name) {
            case "username":
                return entity.getUsername();
            case "email":
                return entity.getEmail();
            case "firstName":
                return entity.getFirstname();
            case "lastName":
                return entity.getLastname();
            case "CREATED_TIMESTAMP":
                return String.valueOf(Instant.parse(entity.getCreatedDate()).getEpochSecond()*1000);
            default:
                return super.getFirstAttribute(name);
        }

    }

    @Override
    public Map<String, List<String>> getAttributes() {
        logger.debug("getAttributes");

        MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();

        attributes.add(UserModel.USERNAME, getUsername());
        attributes.add(UserModel.EMAIL, getEmail());
        attributes.add(UserModel.FIRST_NAME, getFirstName());
        attributes.add(UserModel.LAST_NAME, getLastName());
        return attributes;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        logger.debug("getAttributeStream:" + name);
        List<String> attributeList = new LinkedList<>();
        attributeList.add(getFirstAttribute(name));
        return attributeList.stream();

    }
}
