# Read Me First
The following was discovered as part of building this project:

* The original package name 'com.ndl.numbers-dont-lie' is invalid and this project uses 'com.ndl.numbers_dont_lie' instead.

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/3.5.6/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.5.6/maven-plugin/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.5.6/reference/web/servlet.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

## Rate Limiting Configuration

The application uses a simple in-memory per-user/IP sliding window limiter (`RateLimitFilter`). It is not cluster-safe; for production use a distributed store (Redis / Bucket4j with external persistence).

Configuration (in `application.yml` under `app.rate-limit`):

```
app:
	rate-limit:
		default-per-minute: 120   # Base requests per minute for any key (email if authenticated else IP)
		window-seconds: 60        # Window length in seconds
		override:
			auth2faVerify: 10       # /auth/2fa/verify endpoint stricter limit
			twofaVerifySetup: 10    # /2fa/verify-setup endpoint stricter limit
```

On exceeding the limit a `429` JSON response is returned:

```json
{"error":"rate_limited","limitPerMinute":120,"retryAfterSec":<seconds>}
```

The front-end waits at least 60 seconds before retrying after a 429.

Scaling suggestions:
1. Replace the filter with a Redis-backed token bucket.
2. Externalize limits to config service or environment variables.
3. Separate user vs IP vs global buckets if needed.

