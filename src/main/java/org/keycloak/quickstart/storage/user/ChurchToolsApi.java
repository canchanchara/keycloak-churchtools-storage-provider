package org.keycloak.quickstart.storage.user;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.quickstart.storage.user.churchtools.model.LoginDto;
import org.keycloak.quickstart.storage.user.churchtools.model.PersonDto;
import org.keycloak.quickstart.storage.user.churchtools.model.PersonListDto;
import org.keycloak.quickstart.storage.user.churchtools.model.SearchResultDto;
import org.keycloak.quickstart.storage.user.churchtools.model.ServerCredentials;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChurchToolsApi {

    private ChurchToolsApi() {

    }

    private static final Logger logger = Logger.getLogger(ChurchToolsApi.class);


    public static UserEntity getUserByEmailOrUsername(ServerCredentials serverCredentials, CookieManager cookieManager, String email) {

        logger.info("Find getUserByEmail: " + email);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://"+serverCredentials.getInstance()+".church.tools/api/search?query=" + email + "&domainTypes[]=person"))
                    .headers("Content-Type", "text/plain;charset=UTF-8")
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());


            SearchResultDto searchResultDto = createMapper().readValue(response.body(), SearchResultDto.class);

            String personId = searchResultDto.getData().get(0).getDomainIdentifier();
            return getUserById(serverCredentials, cookieManager, personId);

        } catch (URISyntaxException | InterruptedException | IOException e) {
            logger.error(e);
            throw new ChurchToolsException(e.getMessage());
        }
    }

    public static UserEntity getUserById(ServerCredentials serverCredentials, CookieManager cookieManager, String id) {

        logger.info("getUserById id:" + id);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://"+serverCredentials.getInstance()+".church.tools/api/persons/" + id))
                    .headers("Content-Type", "text/plain;charset=UTF-8")
                    .GET()
                    .build();


            HttpResponse<String> response = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());


            PersonListDto personListDto = createMapper().readValue(response.body(), PersonListDto.class);

            PersonDto personDto = personListDto.getData();

            UserEntity userEntity = new UserEntity();
            userEntity.setId(personDto.getId());
            userEntity.setEmail(personDto.getEmail());
            userEntity.setUsername(personDto.getCmsUserId());

            return userEntity;

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
                    .uri(new URI("https://"+serverCredentials.getInstance()+".church.tools/api/login"))
                    .headers("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

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

            logger.info("Login to "+serverCredentials.getInstance());

            LoginDto loginDto = new LoginDto();
            loginDto.setUsername(serverCredentials.getUsername());
            loginDto.setPassword(serverCredentials.getPassword());
            loginDto.setRememberMe(true);

            String loginJson = createMapper().writeValueAsString(loginDto);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://"+serverCredentials.getInstance()+".church.tools/api/login"))
                    .headers("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginJson))
                    .build();

            CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

            HttpResponse<String> response = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 200) {
                throw new RuntimeException("Ungültiger Login! "+response.body());
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
