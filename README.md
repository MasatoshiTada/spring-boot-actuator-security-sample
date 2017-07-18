Spring Boot Actuator Security Sample
====================================

目標時間：15分

> 以下の手順は、Spring Boot 1.5以降で実行してください。

# 手順1. プロジェクトの作成

`spring-boot-starter-web` と `spring-boot-starter-actuator` を依存性に含めます。

# 手順2. デフォルトのままで起動

- プロジェクトを起動します。デフォルトの状態では、actuatorのエンドポイントにアクセス制限がかかっています。
- sensitiveなエンドポイント(例: `/mappings` )にアクセスすると、レスポンスは401(未認証)になります。

```bash
$ curl -v -X GET http://localhost:8080/mappings | jq
> GET /mappings HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.49.0
> Accept: */*
>
< HTTP/1.1 401
< X-Application-Context: application
< Content-Type: application/vnd.spring-boot.actuator.v1+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Mon, 17 Jul 2017 05:08:30 GMT
<
{
  "timestamp":1500268110405,
  "status":401,
  "error":"Unauthorized",
  "message":"Full authentication is required to access this resource.",
  "path":"/mappings"
}
```

- sensitiveでないエンドポイント(例: `/health` )にアクセスすると、ステータスコードは200(OK)になりますが、一部の情報しか取得できません。

```bash
$ curl -v -X GET http://localhost:8080/health | jq
> GET /health HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.49.0
> Accept: */*
>
< HTTP/1.1 200
< X-Application-Context: application
< Content-Type: application/vnd.spring-boot.actuator.v1+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Mon, 17 Jul 2017 05:10:13 GMT
<
{
  "status":"UP"
}
```

> 各エンドポイントがデフォルトでsensitiveか否かはリファレンスに記載されています。
> http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#production-ready-endpoints

# 手順3. sensitive設定の変更

- application.propertiesに下記の設定を記述し、 `/mappings` を非sensitiveに、 `/health` をsensitiveに変更します。

```properties
endpoints.mappings.sensitive=false
endpoints.health.sensitive=true
```

- プロジェクトを再起動します。
- `/mappings` にアクセスすると、ステータスコードが200(OK)になり、情報が取得できることが分かります。

```bash
$ $ curl -v -X GET http://localhost:8080/mappings | jq
> GET /mappings HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.51.0
> Accept: */*
>
< HTTP/1.1 200
< X-Application-Context: application
< Content-Type: application/vnd.spring-boot.actuator.v1+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Tue, 18 Jul 2017 01:48:43 GMT
<
{
  "/webjars/**": {
    "bean": "resourceHandlerMapping"
  },
  "/**": {
    "bean": "resourceHandlerMapping"
  },
  "/**/favicon.ico": {
    "bean": "faviconHandlerMapping"
  },
  "{[/error]}": {
    "bean": "requestMappingHandlerMapping",
    "method": "public org.springframework.http.ResponseEntity<java.util.Map<java.lang.String, java.lang.Object>> org.springframework.boot.autoconfigure.web.BasicErrorController.error(javax.servlet.http.HttpServletRequest)"
  },
  (以下省略)
}
```

- `/health` にアクセスすると、ステータスコードが401(未認証)になり、情報が取得できなくなったことが分かります。

```bash
$ curl -v -X GET http://localhost:8080/health | jq
> GET /health HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.51.0
> Accept: */*
>
< HTTP/1.1 401
< X-Application-Context: application
< Content-Type: application/vnd.spring-boot.actuator.v1+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Tue, 18 Jul 2017 01:52:31 GMT
<
{
  "timestamp": 1500342751450,
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource.",
  "path": "/health"
}
```

- application.propertiesの記述をコメントアウト(または削除)します。これにより、設定をデフォルト( `/mappings` はsensitive、 `/health` は非sensitive)に戻します。

```properties
#endpoints.mappings.sensitive=false
#endpoints.health.sensitive=true
```

- プロジェクトを再起動します。
- `/mappings` と `/health` にアクセスして、sensitiveの設定が元に戻ったことを確かめてください。

# 手順4. セキュリティ設定の追加

- `spring-boot-starter-security` を依存性に追加します。

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
```

- `USER` ロールのユーザーと、 `ACTUATOR` ロールのユーザーを作成します。

```java
@SpringBootApplication
public class SpringBootActuatorSecuritySampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootActuatorSecuritySampleApplication.class, args);
    }
    
    // 下記のコードを追加する
    @Configuration
    static class SecurityConfig extends WebSecurityConfigurerAdapter {
        
        /**
         * ログイン可能なユーザーを定義するメソッド
         */
        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication()
                    // ユーザー名:actuator、パスワード:password、ロール:ACTUATORのユーザーを追加
                    .withUser("actuator").password("password").roles("ACTUATOR").and()
                    // ユーザー名:user、パスワード:password、ロール:USERのユーザーを追加
                    .withUser("user").password("password").roles("USER");
        }
    }
}
```

> Spring Boot 1.5以降では、 `ACTUATOR` ロールを持つユーザーのみsensitiveなエンドポイントにアクセスできるようになりました。
> http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready-sensitive-endpoints

# 手順5. `ACTUATOR` ロールでのアクセス

- プロジェクトを再起動します。
- `ACTUATOR` ロールでsensitiveなエンドポイントにアクセスします。ステータスコードが200(OK)になり、情報が取得できていることが分かります。

```bash
$ curl -v -u actuator:password -X GET http://localhost:8080/mappings | jq
> GET /mappings HTTP/1.1
> Host: localhost:8080
> Authorization: Basic YWN0dWF0b3I6cGFzc3dvcmQ=
> User-Agent: curl/7.49.0
> Accept: */*
>
< HTTP/1.1 200
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 1; mode=block
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Frame-Options: DENY
< Set-Cookie: JSESSIONID=341B50A1D61F38C4E8A7F43F1096CD02; Path=/; HttpOnly
< X-Application-Context: application
< Content-Type: application/vnd.spring-boot.actuator.v1+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Mon, 17 Jul 2017 05:23:49 GMT
<
{
  "/webjars/**": {
    "bean": "resourceHandlerMapping"
  },
  "/**": {
    "bean": "resourceHandlerMapping"
  },
  "/**/favicon.ico": {
    "bean": "faviconHandlerMapping"
  },
  "{[/error]}": {
    "bean": "requestMappingHandlerMapping",
    "method": "public org.springframework.http.ResponseEntity<java.util.Map<java.lang.String, java.lang.Object>> org.springframework.boot.autoconfigure.web.BasicErrorController.error(javax.servlet.http.HttpServletRequest)"
  },
  (以下省略)
}
```

- `ACTUATOR` ロールでsensitiveでないエンドポイントにアクセスします。ステータスコードは変わらず200(OK)ですが、セキュリティ設定をする前より、多くの情報が取得できていることが分かります。

```bash
$ curl -v -u actuator:password -X GET http://localhost:8080/health | jq
> GET /health HTTP/1.1
> Host: localhost:8080
> Authorization: Basic YWN0dWF0b3I6cGFzc3dvcmQ=
> User-Agent: curl/7.49.0
> Accept: */*
>
< HTTP/1.1 200
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 1; mode=block
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Frame-Options: DENY
< Set-Cookie: JSESSIONID=0886438EA414B573CACB6028F3889ADA; Path=/; HttpOnly
< X-Application-Context: application
< Content-Type: application/vnd.spring-boot.actuator.v1+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Mon, 17 Jul 2017 05:27:39 GMT
<
{
  "status": "UP",
  "diskSpace": {
    "status": "UP",
    "total": 120007426048,
    "free": 51565465600,
    "threshold": 10485760
  }
}
```

> エンドポイントにアクセス可能なロール名を変更したい場合は、application.propertiesに下記の設定を記述します。
> ```properties
> management.security.roles=ロール名1,ロール名2,...
> ```

# 手順6. `ACTUATOR` 以外のロールでのアクセス

- `USER` ロールでsensitiveなエンドポイントにアクセスします。 `ACTUATOR` ロールではないので情報は取得できませんが、ステータスコードが401(未認証)ではなく403(アクセス権なし)になっていることが分かります。

```bash
$ curl -v -u user:password -X GET http://localhost:8080/mappings | jq
> GET /mappings HTTP/1.1
> Host: localhost:8080
> Authorization: Basic dXNlcjpwYXNzd29yZA==
> User-Agent: curl/7.49.0
> Accept: */*
>
< HTTP/1.1 403
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 1; mode=block
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Frame-Options: DENY
< Set-Cookie: JSESSIONID=5F9686BEA6CA4FF5B064CDA5D2083AF5; Path=/; HttpOnly
< X-Application-Context: application
< Content-Type: application/vnd.spring-boot.actuator.v1+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Mon, 17 Jul 2017 05:29:13 GMT
<
{
  "timestamp": 1500269353497,
  "status": 403,
  "error": "Forbidden",
  "message": "Access is denied. User must have one of the these roles: ACTUATOR",
  "path": "/mappings"
}
```

- `USER` ロールでsensitiveでないエンドポイントにアクセスします。sensitiveでないので、 `ACTUATOR` ロールでなくてもアクセスできますが、一部の情報しか取得できていないことが分かります。

```bash
$ curl -v -u user:password -X GET http://localhost:8080/health | jq
> GET /health HTTP/1.1
> Host: localhost:8080
> Authorization: Basic dXNlcjpwYXNzd29yZA==
> User-Agent: curl/7.49.0
> Accept: */*
>
< HTTP/1.1 200
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 1; mode=block
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Frame-Options: DENY
< Set-Cookie: JSESSIONID=4C686AE7FBD659BB6D990D6047F5386D; Path=/; HttpOnly
< X-Application-Context: application
< Content-Type: application/vnd.spring-boot.actuator.v1+json;charset=UTF-8
< Transfer-Encoding: chunked
< Date: Mon, 17 Jul 2017 05:35:36 GMT
<
{
  "status": "UP"
}
```

手順は以上です。