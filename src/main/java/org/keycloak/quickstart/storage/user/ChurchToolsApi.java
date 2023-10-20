package org.keycloak.quickstart.storage.user;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.quickstart.storage.user.churchtools.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ChurchToolsApi {

    private static final Logger logger = Logger.getLogger(ChurchToolsApi.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String host;
    private final String loginToken;

    private ChurchToolsApi(String host, String loginToken) {
        httpClient = HttpClient.newHttpClient();
        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.host = host;
        this.loginToken = loginToken;
    }

    public static ChurchToolsApi createWithLoginToken(String host, String loginToken) {
        return new ChurchToolsApi(host, loginToken);
    }

    public PersonDto getUserByEmailOrUsername(String identifier) {

        logger.info("Find getUserByEmail: " + identifier);

        SearchResultDto searchResultDto =
                get("/api/search?query=" + identifier + "&domainTypes[]=person", SearchResultDto.class);

        // Search may return multiple people for the same username, but we need an exact match
        for (SearchResultDataDto searchResult : searchResultDto.getData()) {
            PersonDto personDto = getUserById(searchResult.getDomainIdentifier());
            if (personDto != null && (personDto.getEmail().equals(identifier) || personDto.getCmsUserId().equals(identifier)))
                return personDto;
        }

        return null;
    }

    public PersonDto getUserById(String id) {

        logger.info("getUserById id:" + id);

        SinglePersonListDto personListDto = get("/api/persons/" + id, SinglePersonListDto.class);

        return personListDto.getData();
    }

    public Integer getPersonCount() {
        logger.info("getPersonCount");

        PersonListDto personListDto =
                get("/api/persons?is_archived=false&page=" + 1 + "&limit=" + 1, PersonListDto.class);

        return personListDto.getMeta().getCount();
    }


    public List<String> findPersonsBySearchTerm(String userSearchTerm) {

        logger.info("findPersonsBySearchTerm: " + userSearchTerm);

        SearchResultDto searchResultDto =
                get("/api/search?query=" + userSearchTerm + "&domainTypes[]=person", SearchResultDto.class);

        return searchResultDto.getData().stream().map(SearchResultDataDto::getDomainIdentifier).toList();
    }

    /**
     * liefert maximal 500 Personen zurück
     */
    public List<PersonDto> findPersons(String userSearchTerm, Integer firstResult, Integer maxResults) {

        logger.info("findPersons searchTerm:" + userSearchTerm + " firstResult: " + firstResult + " maxResults: " + maxResults);

        StringBuilder personFilter = new StringBuilder();

        if (StringUtils.isNotEmpty(userSearchTerm)) {
            List<String> personIds = findPersonsBySearchTerm(userSearchTerm);
            for (String personId : personIds) {
                personFilter.append("&ids%5B%5D=");
                personFilter.append(personId);
            }
        }

        PersonListDto personListDto = get("/api/persons?is_archived=false&page=" + 1 + "&limit=" + 500 + personFilter, PersonListDto.class);

        return personListDto.getData().stream()
                .filter(p -> StringUtils.isNotEmpty(p.getCmsUserId()))
                .skip(firstResult == null ? 0 : firstResult)
                .limit(maxResults == null ? 1000 : maxResults)
                .toList();
    }

    public boolean credentialsValid(String identifier, String password) {

        try {
            LoginDto loginDto = new LoginDto();
            loginDto.setUsername(identifier);
            loginDto.setPassword(password);
            loginDto.setRememberMe(false);

            String loginJson = objectMapper.writeValueAsString(loginDto);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://" + host + "/api/login"))
                    .headers("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            /*
             * Falls totp aktiviert ist, wird der Login hier trotzdem zugelassen
             * TODO: Per Property das verhalten bestimmen lassen
             */
            boolean loginValid = response.statusCode() == 200;

            logger.info("credentialsValid: " + loginValid);
            return loginValid;

        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error(e);
            throw new ChurchToolsException(e.getMessage(), e);
        }

    }

    private <T> T get(String path, Class<T> bodyType) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://" + host + path))
                    .headers("Authorization", "Login " + loginToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), bodyType);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error(e);
            throw new ChurchToolsException(e.getMessage(), e);
        }
    }
}
