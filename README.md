# aws-secrets-parameter-client

## Overview

This Java library helps fetch configuration values from AWS Systems Manager Parameter Store and AWS Secrets Manager. The
library allows users to retrieve parameter store values based on predefined prefixes and map them to property file keys.
Additionally, it fetches credentials like username and password from AWS Secrets Manager for a given secret name.

## Features

- Fetch parameter store values based on predefined prefixes and map them to property file keys.
- Fetch credentials like username and password from AWS Secrets Manager for a given secret name.
- Support for retrieving values from both AWS Parameter Store and AWS Secrets Manager.
- Easy integration with Spring Boot and Spring Cloud.

## Installation

Add the following dependency to your project's `pom.xml`:

```xml

<dependency>
    <groupId>com.aws.config</groupId>
    <artifactId>aws-to-application.properties</artifactId>
    <version>${version}</version>
    <scope>compile</scope>
</dependency>
```

## Usage

In your Spring Boot application's `application.properties`, set the `aws.parameterstore.config.enabled` property to
`true` to enable AWS configuration.

```properties
aws.parameterstore.config.enabled=true
```

#### Behavior Based on the Configuration

- If `aws.parameterstore.config.enabled` is set to `true`, the library will fetch parameter store values based on
  predefined prefixes and map them to property file keys.

- If `aws.parameterstore.config.enabled` is set to `false`, the application will use default properties instead of
  fetching values from AWS.

### Fetching Parameter Store Values

To retrieve values from AWS Parameter Store, you need to define a secret name:

```properties
aws.parameterstore.secretName.test=/test
```

#### How It Works

- The library fetches the JSON object stored in the AWS Parameter Store for the given secret name.
- The JSON format should be:

```json
{
  "key": "value"
}
```

- The library maps each key from the JSON response to the corresponding property file format using the same suffix:
  `aws.parameterstore.key = value`.

### Example Configuration

```properties
aws.parameterstore.config.enabled=true
aws.parameterstore.secretName.appConfig=/app/config
```

- If the Parameter Store `/app/config` contains:

```json
{
  "dbUrl": "jdbc:mysql://localhost:3306/mydb",
  "dbUser": "admin"
}
```

- The library will automatically map it to:

```properties
aws.parameterstore.dbUrl=jdbc:mysql://localhost:3306/mydb
aws.parameterstore.dbUser=admin
```

### Fetching Secrets from AWS Secrets Manager

To retrieve a username and password from AWS Secrets Manager, follow these steps:

- First, fetch the secret name from the Parameter Store:

```properties
aws.parameterstore.dbsecret=my-db-secret
```

- The library maps the retrieved value to:

```properties
aws.secretsmanager.secretName.dbsecret=my-db-secret
```

- The library fetches the secret value (which is usually a JSON object) from AWS Secrets Manager. The expected JSON
  format is:

```json
{
  "username": "admin",
  "password": "secret-password"
}
```

- The library automatically maps the retrieved values to properties with the same suffix:

```properties
aws.secretsmanager.dbsecret.username=admin
aws.secretsmanager.dbsecret.password=secret-password
```

### Security Considerations

- Do not store secrets in plain text. Always use AWS Secrets Manager for sensitive credentials.
- Ensure IAM roles have the least privilege necessary to access Parameter Store and Secrets Manager.
- Rotate secrets regularly to enhance security.

### Contribution & Support

- If you want to contribute, submit issues or pull requests on the project repository.
- For support, reach out via email or project discussions.

### License & Disclaimer

- This project is licensed under the `Apache 2.0 License`.
- Ensure you follow AWS security best practices while using this library.