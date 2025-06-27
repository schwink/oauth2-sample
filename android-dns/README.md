The OAuth2 server configures various hostnames which the Android emulator needs to be told to resolve to the host computer's IP address.

To start this DNS server:
```
docker-compose -f docker-compose.yaml up
```

To start the emulator configured to use this DNS server:
```
emulator -list-avds
emulator -avd Medium_Phone_API_36.0 -no-snapshot-load -dns-server 127.0.0.1
```
