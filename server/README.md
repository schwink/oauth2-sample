
# Authentication example with oauth2-proxy

Starting from this example:
https://github.com/oauth2-proxy/oauth2-proxy/blob/master/contrib/local-environment/docker-compose-nginx.yaml

To run it:
```
docker-compose -f docker-compose.yaml -f docker-compose-nginx.yaml up
```

Visit http://httpbin.oauth2-proxy.localhost/
