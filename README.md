# Example local OAuth2 setup for web and mobile clients

This sample repository unites [OpenID AppAuth](https://github.com/openid), [oauth2-proxy](https://github.com/oauth2-proxy/oauth2-proxy), and [Dex](https://dexidp.io) into an end-to-end system that supports both web browser cookie and Authorization header auth:
- An OpenID Connect identity provider at http://dex.oauth2-proxy.localtest.me
- An example user-facing web site at http://httpbin.oauth2-proxy.localtest.me
- An example REST service at http://api.oauth2-proxy.localtest.me/status
- An Android app showing how to manage login in a modern Jetpack Compose app

All web services are configured to be hosted locally with [Docker](https://docs.docker.com/desktop/) for test purposes; localtest.me is a standard example domain that DNS resolves to 127.0.0.1.

## Why would I use this?

- Easy local development of early-stage apps, before taking a dependency on a cloud service like [Firebase](https://firebase.google.com/docs/auth/android/firebaseui), etc.
- Self host a basic system for personal projects. The setup demonstrated here does not have any affordances for sign up or account creation, but could be ideal for adding auth to hobby services with a small audience.

## The Dex identity provider

(Dex)[https://dexidp.io] is a flexible open source identity service. It is responsible for talking the OpenID Connect protocol to other parts of the system (i.e. managing their login tokens), and delegates to another service to determine the identities and credentials of users. In this demo, it just uses a hard-coded example account in its config file.

## Web service reverse proxy with Nginx and oauth2-proxy

http://oauth2-proxy.localtest.me and its subdomains are served by Nginx running locally.

[nginx.conf](https://github.com/schwink/oauth2-scaffold/blob/main/server/nginx.conf) has a `server_name` entry for each.

It uses Nginx's [`auth_request`](https://nginx.org/en/docs/http/ngx_http_auth_request_module.html) feature to test each request against the local oauth2-proxy web service. If the request headers contain valid JWT credentials (in the cookie or `Authentication` headers), the request is allowed. Otherwise, the request is either failed with the appropriate error code (the API) or redirected to a login page (the web site).

## Android app

In the Android emulator, we need localtest.me to resolve to an IP address pointing to the host machine that is running Docker. To achieve this, we host a local DNS service defined in the [`android-dns/` subdirectory](https://github.com/schwink/oauth2-scaffold/tree/main/android-dns) to define this mapping.

The Android app makes use of the [AppAuth Android](https://github.com/openid/AppAuth-Android) project from OpenID to acquire tokens from Dex.

In this demo we use only clear text http, so the app disables various validation ([[1]](https://github.com/schwink/oauth2-scaffold/blob/main/android/app/src/main/java/com/example/oauth2sample/app/auth/UserSessionService.kt#L122-L135), [[2]](https://github.com/schwink/oauth2-scaffold/blob/main/android/app/src/main/AndroidManifest.xml#L17C45-L17C68),[[3]](https://github.com/schwink/oauth2-scaffold/blob/main/android/app/src/main/res/xml/network_security_config.xml)) in the AppAuth library. If you deploy any of this on the internet, you must use HTTPS!

## iOS app

TODO!
