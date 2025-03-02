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
