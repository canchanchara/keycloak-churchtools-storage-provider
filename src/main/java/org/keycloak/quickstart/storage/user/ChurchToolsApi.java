package org.keycloak.quickstart.storage.user;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.quickstart.storage.user.churchtools.model.*;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ChurchToolsApi {

    public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain;charset=UTF-8";
    public static final String CONTENT_TYPE = "Content-Type";

    private ChurchToolsApi() {

    }

    private static final Logger logger = Logger.getLogger(ChurchToolsApi.class);


    public static PersonDto getUserByEmailOrUsername(ServerCredentials serverCredentials, CookieManager cookieManager, String email) {

        logger.info("Find getUserByEmail: " + email);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://" + serverCredentials.getInstance() + ".church.tools/api/search?query=" + email + "&domainTypes[]=person"))
                    .headers(CONTENT_TYPE, CONTENT_TYPE_TEXT_PLAIN)
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());


            SearchResultDto searchResultDto = createMapper().readValue(response.body(), SearchResultDto.class);

            if(CollectionUtils.isEmpty(searchResultDto.getData())) {
                return null;
            }

            String personId = searchResultDto.getData().get(0).getDomainIdentifier();
            return getUserById(serverCredentials, cookieManager, personId);

        } catch (URISyntaxException | InterruptedException | IOException e) {
            logger.error(e);
            throw new ChurchToolsException(e.getMessage());
        }
    }

    public static PersonDto getUserById(ServerCredentials serverCredentials, CookieManager cookieManager, String id) {

        logger.info("getUserById id:" + id);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://" + serverCredentials.getInstance() + ".church.tools/api/persons/" + id))
                    .headers(CONTENT_TYPE, CONTENT_TYPE_TEXT_PLAIN)
                    .GET()
                    .build();


            HttpResponse<String> response = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return null;

            SinglePersonListDto personListDto = createMapper().readValue(response.body(), SinglePersonListDto.class);

            return personListDto.getData();

        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error(e);
            throw new ChurchToolsException(e.getMessage());
        }
    }

    public static Integer getPersonCount(ServerCredentials serverCredentials, CookieManager cookieManager) {
        logger.info("getPersonCount");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://" + serverCredentials.getInstance() + ".church.tools/api/persons?is_archived=false&page=" + 1 + "&limit=" + 1))
                    .headers(CONTENT_TYPE, CONTENT_TYPE_TEXT_PLAIN)
                    .GET()
                    .build();


            HttpResponse<String> response = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());


            PersonListDto personListDto = createMapper().readValue(response.body(), PersonListDto.class);

            return personListDto.getMeta().getCount();


        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error(e);
            throw new ChurchToolsException(e.getMessage());
        }
    }


    public static List<String> findPersonsBySearchTerm(ServerCredentials serverCredentials, CookieManager cookieManager, String userSearchTerm) {

        logger.info("findPersonsBySearchTerm: " + userSearchTerm);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://" + serverCredentials.getInstance() + ".church.tools/api/search?query=" + userSearchTerm + "&domainTypes[]=person"))
                    .headers(CONTENT_TYPE, CONTENT_TYPE_TEXT_PLAIN)
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            SearchResultDto searchResultDto = createMapper().readValue(response.body(), SearchResultDto.class);

            return searchResultDto.getData().stream().map(SearchResultDataDto::getDomainIdentifier).toList();

        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error(e);
            throw new ChurchToolsException(e.getMessage());
        }
    }

    /**
     * liefert maximal 500 Personen zurück
     */
    public static List<PersonDto> findPersons(ServerCredentials serverCredentials, CookieManager cookieManager, String userSearchTerm, Integer firstResult, Integer maxResults) {

        String personFilter = "";

        if (StringUtils.isNotEmpty(userSearchTerm)) {
            List<String> personIds = findPersonsBySearchTerm(serverCredentials, cookieManager, userSearchTerm);
            for (String personId : personIds) {
                if (personFilter.equals("")) {
                    personFilter = "ids%5B%5D=" + personId;
                } else {
                    personFilter = personFilter + "&ids%5B%5D=" + personId;
                }
            }
        }

        logger.info("findPersons searchterm:" + userSearchTerm + " firstResult: " + firstResult + " maxResults: " + maxResults);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://" + serverCredentials.getInstance() + ".church.tools/api/persons?is_archived=false&page=" + 1 + "&limit=" + 500 + "&" + personFilter))
                    .headers(CONTENT_TYPE, CONTENT_TYPE_TEXT_PLAIN)
                    .GET()
                    .build();


            HttpResponse<String> response = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            PersonListDto personListDto = createMapper().readValue(response.body(), PersonListDto.class);

            List<PersonDto> personList = personListDto.getData();

            return personList.stream()
                    .filter(p -> StringUtils.isNotEmpty(p.getCmsUserId()))
                    .skip(firstResult == null ? 0 : firstResult)
                    .limit(maxResults == null ? 1000 : maxResults)
                    .toList();


        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error(e);
            throw new ChurchToolsException(e.getMessage());
        }
    }

    public static boolean credentialsValid(ServerCredentials serverCredentials, String email, String passwort) {
        try {

            LoginDto loginDto = new LoginDto();
            loginDto.setUsername(email);
            loginDto.setPassword(passwort);
            loginDto.setRememberMe(false);

            String loginJson = createMapper().writeValueAsString(loginDto);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://" + serverCredentials.getInstance() + ".church.tools/api/login"))
                    .headers(CONTENT_TYPE, "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            /**
             * Falls totp aktiviert ist, wird der Login hier trotzdem zugelassen
             * TODO: Per Property das verhalten bestimmen lassen
             */
            boolean loginValid = response.statusCode() == 200;

            logger.info("credentialsValid: " + loginValid);
            return loginValid;

        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error(e);
            throw new ChurchToolsException(e.getMessage());
        }

    }

    public static CookieManager login(ServerCredentials serverCredentials) {

        try {

            logger.info("Login to " + serverCredentials.getInstance());

            LoginDto loginDto = new LoginDto();
            loginDto.setUsername(serverCredentials.getUsername());
            loginDto.setPassword(serverCredentials.getPassword());
            loginDto.setRememberMe(true);

            String loginJson = createMapper().writeValueAsString(loginDto);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://" + serverCredentials.getInstance() + ".church.tools/api/login"))
                    .headers(CONTENT_TYPE, "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                    .build();

            CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

            HttpResponse<String> response = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Ungültiger Login! " + response.body());
            }

            return cookieManager;
        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error(e);
            throw new ChurchToolsException(e.getMessage());
        }

    }

    private static ObjectMapper createMapper() {
        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

}
