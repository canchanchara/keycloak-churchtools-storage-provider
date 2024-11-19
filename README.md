# Keycloak ChurchTools Storage Provider

Keycloak erlaubt es externe Datenquellen anzubinden, in der die Benutzerdaten wie Benutzername und Passwort stehen.
ChurchTools bietet eine REST API, um Daten auszulesen und sich einzuloggen.

Dieses kleine Projekt verbindet diese beide Funktionen, sodass man sich per OpenID Connect mit dem ChurchTools
Account in allen Systemen einloggen kann, die OpenID Connect unterstützen.
Darunter zählen Systeme wie WordPress, Synology oder Nextcloud.

Getestet mit Keycloak 26.0.5.

## Installation

### ChurchTools Konto für Keycloak

Zunächst muss ein ChurchTools Konto für diesen Storage Provider erstellt werden:

1. Eine Person mit E-Mail-Adresse in ChurchTools hinzufügen.  
   Für solche API Benutzer bietet sich außerdem ein eigener Status (Mitglied, Freund, API-Nutzer) an
2. Die neu erstellte Person zu ChurchTools einladen.
3. Auf den Einladungslink klicken und ein Passwort festlegen.
4. Im Browser https://example.church.tools/api/whoami öffnen, um die ID der neuen Person nachzuschlagen.
5. Im Browser https://example.church.tools/api/person/${ID}/logintoken öffnen, 
   um das zeitlich unbegrenzt gültige Login Token nachzuschlagen.

Schritt 4. und 5. können auch über die Weboberfläche ausgeführt werden: [Official Documentation](https://hilfe.church.tools/wiki/0/API%20Authentifizierung#logintoken)

> ⚠️ Aus Sicherheitsgründen sollte dieser Storage Provider nicht mit einem ChurchTools Administrator Konto betrieben werden.

Folgende Berechtigungen für das neu erstellte Konto gewähren:

- Personen & Gruppen > "Personen & Gruppen" sehen `churchdb:view`
- Personen & Gruppen > Sicherheitslevel Personendaten (Stufe 1-3) `churchdb:security level person(1,2,3)`
- Personen & Gruppen > Alle Personen des jeweiligen Bereiches sichtbar machen (Alle) `churchdb:view alldata(-1)`

### Keycloak mit Docker Compose installieren

Da es Zeit erfordert, sich in eine sichere Einrichtung von Keycloak mit Docker einzuarbeiten,
bieten wir ein [Compose File](compose.yaml) als Orientierung an.
In einer Umgebung mit NGINX Reverse Proxy ist es ausreichend, `user-storage-churchtools.jar` aus den
[Releases](https://github.com/canchanchara/keycloak-churchtools-storage-provider/releases) herunterzuladen,
die Passwörter und ChurchTools Zugangsdaten im Compose File anzupassen und es ansonsten unverändert zu übernehmen.

### Keycloak traditionell installieren

Anschließend muss die `user-storage-churchtools.jar` aus den
[Releases](https://github.com/canchanchara/keycloak-churchtools-storage-provider/releases)
heruntergeladen und in `keycloak/providers` gespeichert werden.

Die ChurchTools Zugangsdaten aus dem ersten Schritt müssen anschließend z.B. in `keycloak/conf/keycloak.conf`
konfiguriert werden.

```properties
spi-storage-churchtools-user-storage-host=demo.church.tools
spi-storage-churchtools-user-storage-login-token=DuIojxSyqCIM...rfH1foJvwax
```

### Ersteinrichtung von Keycloak

Nach dem ersten Start von Keycloak empfiehlt es sich, einen eigenen Realm für ChurchTools zu erstellen
und anschließend unter _User Federation_ den Provider `Churchtools-user-storage` hinzufügen. 

Anschließend kann man testweise unter _Users_ nach `*` suchen. Dann sieht man alle Personen,
die sich künftig über Keycloak bei Drittanwendungen anmelden können.

Solche Drittanwendungen wie Nextcloud, WordPress, usw. kann man unter _Clients_ hinzufügen.

## Einschränkungen

### 2FA

Dieser Storage Provider ignoriert Zwei-Faktor-Authentifizierung von ChurchTools.
Benutzername und Passwort sind zum Anmelden in Keycloak ausreichend. 
Falls 2FA zum Einsatz kommen soll, muss außerdem
[keycloak-churchtools-totp-provider](https://github.com/canchanchara/keycloak-churchtools-totp-provider)
eingerichtet werden. 

### Kompatibilität

Die Benutzer ID (`sub` claim) von Keycloak entspricht dem Format `f:<keycloak uuid>:<churchtools id>`.
Bei manchen Anwendungen sorgen die Doppelpunkte allerdings für Probleme: 
- [Nextcloud (user_oidc)](https://github.com/nextcloud/user_oidc/issues/690)

## Entwicklung

Das Projekt kann mit Java 21 und Maven gebaut werden.

```bash
mvn clean install
```

Anschließend findet man die JAR-Datei unter `target/user-storage-churchtools.jar`.
