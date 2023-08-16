# Church Tools Custom Storage Provider

Keycloak erlaubt es externe Datenquellen anzubinden, in der die Benutzerdaten wie Benutzername und Passwort stehen.
Church Tools bietet eine REST API, um Daten auszulesen und sich einzuloggen.

Dieses kleine Projekt verbindet diese beide Funktionen, sodass man sich per OpenID Connect mit dem Church Tools
Account in allen Systemen einloggen kann, die OpenID Connect unterstützen.
Darunter zählen zb. Systeme wie Wordpress oder Synology oder Nextcloud.

Getestet mit Keycloak 22.01.

## Installation

Die Churchtools spezifischen Daten wie die Instanz und Username/Passwort eines Adminusers sind in der keycloak.conf 
Datei zu hinterlegen.

keycloak/conf/keycloak.conf

```
spi-storage-churchtools-user-storage-instance=churchtoolsname
spi-storage-churchtools-user-storage-username=adminuser
spi-storage-churchtools-user-storage-password=adminpassword
```

Das Projekt kann mit Java 17 und Maven gebaut werden.

```bash
mvn clean install
```

Die jar Datei aus dem Targetverzeichnis muss in das providers Verzeichnis von keycloak kopiert werden.

```
cp target/user-storage-churchtools.jar keycloak/providers
```

Keycloak muss danach neugestartet werden.

Am besten legt man einen eigenen Realm an.
Im Realm kann man unter *User Federation* den Church Tools User Storage hinzufügen.

Anschließend muss man unter *Clients* einen neuen Clienten anlegen.
Diese Zugangsdaten müssen dann im Zielsystem eingetragen werden.

Der Client muss folgende Einstellungen haben:
 * Client type:  OpenID Connect
 * Client authentication: enabled (confidential access type)
 * Client Authenticator (Unter dem Credentialstab): Client Id and Secret


# Prototyp

Das Projekt ist ein Prototyp, um den Durchstich zu testen. 
Für einen produktiven Einsatz müssen ggf. weitere Funktionen ergänzt werden.