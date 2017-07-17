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

- sensitiveでないエンドポイント(例: `/health` )にアクセスすると、ステータスコードは200になりますが、一部の情報しか取得できません。

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

# 手順3. セキュリテイ設定の追加

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

# 手順4. `ACTUATOR` ロールでのアクセス

- プロジェクトを再起動します。
- `ACTUATOR` ロールでsensitiveなエンドポイントにアクセスします。ステータスコードが200になり、情報が取得できていることが分かります。

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
{ [5617 bytes data]
100  5609    0  5609    0     0   791k      0 --:--:-- --:--:-- --:--:-- 1369k
* Connection #0 to host localhost left intact
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

- `ACTUATOR` ロールでsensitiveでないエンドポイントにアクセスします。セキュリテイ設定をする前より、多くの情報が取得できていることが分かります。

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

# 手順5. `USER` ロールでのアクセス

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