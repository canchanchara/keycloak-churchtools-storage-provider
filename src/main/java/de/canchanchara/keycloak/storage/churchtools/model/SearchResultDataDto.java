package de.canchanchara.keycloak.storage.churchtools.model;

public class SearchResultDataDto {

    String title;
    String domainIdentifier;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDomainIdentifier() {
        return domainIdentifier;
    }

    public void setDomainIdentifier(String domainIdentifier) {
        this.domainIdentifier = domainIdentifier;
    }
}
