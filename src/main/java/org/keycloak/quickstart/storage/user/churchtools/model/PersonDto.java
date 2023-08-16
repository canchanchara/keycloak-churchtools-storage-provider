package org.keycloak.quickstart.storage.user.churchtools.model;

public class PersonDto {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String cmsUserId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCmsUserId() {
        return cmsUserId;
    }

    public void setCmsUserId(String cmsUserId) {
        this.cmsUserId = cmsUserId;
    }
}
