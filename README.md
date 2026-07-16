# DynamoDB CRUD API — AWS Full Stack Project

A Spring Boot REST API backed by AWS services, deployed on AWS Lambda and ECS Fargate, with a full CI/CD pipeline and observability stack.

---

## Table of Contents

- [Task 1 — DynamoDB-Backed REST API on AWS Lambda](#task-1--dynamodb-backed-rest-api-on-aws-lambda)
- [Task 2 — Advanced AWS Integrations](#task-2--advanced-aws-integrations)
- [Task 3 — SFTP to XML Pipeline](#task-3--sftp-to-xml-pipeline)
- [Task 4 — CI/CD Pipeline](#task-4--cicd-pipeline)
- [Task 5 — Monitoring, Logging & Alerting](#task-5--monitoring-logging--alerting)
- [AWS Infrastructure Summary](#aws-infrastructure-summary)
- [Cost Note](#cost-note)

---

## Task 1 — DynamoDB-Backed REST API on AWS Lambda

### What was built
A Spring Boot REST API with full CRUD operations backed by Amazon DynamoDB, deployed as an AWS Lambda function behind API Gateway.

### Data Model
`ItemVariants` DynamoDB table:
| Field | Type | Key |
|-------|------|-----|
| itemId | String | Partition Key |
| variantId | String | Sort Key |
| barCode | String | |
| sku | String | |
| countryOfOrigin | String | |

### Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/item-variants` | Create item |
| GET | `/api/item-variants` | Get all items |
| GET | `/api/item-variants/{itemId}/{variantId}` | Get one item |
| PUT | `/api/item-variants/{itemId}/{variantId}` | Update item |
| DELETE | `/api/item-variants/{itemId}/{variantId}` | Delete item |

### Key Technical Decisions
- Used **Maven Shade Plugin** instead of Spring Boot repackage — Lambda requires a flat jar where all classes are at the root. Spring Boot's default fat jar nests classes in `BOOT-INF/classes` which Lambda cannot load.
- Used **HTTP API v2** (`HttpApiV2ProxyRequest`) in API Gateway — not the older REST API v1 format.
- Java 21 runtime on Lambda.

### Live URL
`https://cuoxxwnov4.execute-api.us-east-1.amazonaws.com`

---

## Task 2 — Advanced AWS Integrations

### What was built
Four additional AWS integrations added to the existing API.

### 1. Secrets Manager
- API key stored in AWS Secrets Manager at runtime
- Endpoint: `GET /api/item-variants/config/api-key`

### 2. S3 File Storage
- Upload product details JSON: `POST /api/item-variants/{itemId}/{variantId}/details`
- Retrieve pre-signed URL (10 min expiry): `GET /api/item-variants/{itemId}/{variantId}/details`
- S3 bucket: `item-variant-details-846497880922`

### 3. SQS Event Publishing
- Every `POST` publishes a `ProductCreatedEvent` to SQS
- Every `PUT` publishes a `ProductUpdatedEvent` to SQS
- Message format: `{ eventType, timestamp, payload }`

### 4. SQS Consumer + DLQ Failover
- `item-variant-sqs-processor` Lambda reads events and saves to DynamoDB
- `item-variant-dlq` configured with `maxReceiveCount: 3` — failed messages move to DLQ after 3 retries
- `item-variant-dlq-processor` Lambda handles and reprocesses DLQ messages

### AWS Resources
| Resource | Name |
|----------|------|
| SQS Queue | `item-variant-queue` |
| SQS DLQ | `item-variant-dlq` |
| S3 Bucket | `item-variant-details-846497880922` |
| Secrets Manager | `item-variant-api/config` |
| Lambda | `item-variant-sqs-processor` |
| Lambda | `item-variant-dlq-processor` |

---

## Task 3 — SFTP to XML Pipeline

### What was built
A standalone Spring Boot module (`sftp-processor`) that reads a pipe-delimited flat file from an SFTP server, parses it, converts it to XML, uploads to S3, and sends an SQS notification.

### Pipeline Steps
```
SFTP Server → Download .TXT file → Parse pipe-delimited records → Generate XML → Upload to S3 → SQS notification
```

### Flat File Format
Lines starting with `N` are data records, pipe-delimited:
- `fields[4]` = itemId
- `fields[5]` = sku
- `fields[6]` = artist
- `fields[7]` = title
- `fields[9]` = streetDate
- `fields[12]` = countryOfOrigin
- `fields[18]` = taxProductCode
- `fields[20]` = vendor

### XML Output Format
```xml
<ItemList>
  <Item ItemID="..." OrganizationCode="TEST" UnitOfMeasure="EACH" Action="Manage">
    <PrimaryInformation Description="..." ManufacturerName="..." CountryOfOrigin="..."/>
    <ClassificationCodes TaxProductCode="..."/>
    <AdditionalAttributeList>
      <AdditionalAttribute Name="digital" Value="false"/>
      <AdditionalAttribute Name="music" Value="true"/>
      <AdditionalAttribute Name="taxcode" Value="..."/>
    </AdditionalAttributeList>
    <Extn ExtnSku="..." ExtnStreetDate="..." ExtnVendor="..."/>
  </Item>
</ItemList>
```

### Output
- S3 key: `sftp-output/PROD_OUTPUT_{timestamp}.xml`
- SQS message: `{ event: XML_UPLOADED, s3Key, itemCount, timestamp }`

### SFTP Server
- Provider: sftpcloud.io (free tier)
- Host: `us-east-1.sftpcloud.io`

---

## Task 4 — CI/CD Pipeline

### What was built
A GitHub Actions workflow that automatically builds, tests, containerizes, and deploys the application to AWS ECS Fargate on every push to `main`.

### Pipeline Flow
```
git push → GitHub Actions:
  Job 1: Build & Test (mvn test) ← blocks deploy if tests fail
  Job 2: Build Docker image → Push to ECR → Deploy to ECS Fargate
```

### Workflow File
`.github/workflows/deploy.yml`

### Pipeline Stages
| Stage | What happens |
|-------|-------------|
| Build and Test | Compiles app, runs unit tests. Fails fast — deploy is blocked if tests fail |
| Push to ECR | Builds Docker image tagged with commit SHA, pushes to Amazon ECR |
| Deploy to ECS | Updates task definition with new image, triggers rolling deployment, waits for stability |

### Secrets Management
AWS credentials stored as GitHub Actions Secrets — never hardcoded:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

### Fail-Fast Behavior
`push-deploy` job declares `needs: build-test` — a failed test run completely blocks deployment. No broken build can reach production.

### Docker
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/dynamodb-crud-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-cp", "app.jar", "com.dynamo.api.DynamodbCrudApiApplication"]
```

### AWS Resources
| Resource | Name |
|----------|------|
| ECR Repository | `dynamodb-crud-api` |
| ECS Cluster | `dynamodb-crud-cluster` |
| ECS Service | `dynamodb-crud-service` |
| ECS Task Definition | `dynamodb-crud-task` |
| IAM Task Role | `ecsTaskRole` |
| IAM Execution Role | `ecsTaskExecutionRole` |

### ECR Lifecycle Policy
Keeps only the last 3 images — auto-expires older images to stay within ECR free tier.

---

## Task 5 — Monitoring, Logging & Alerting

### What was built
Full observability stack: structured JSON logging, correlation IDs, CloudWatch alarms, SNS email alerts, a CloudWatch dashboard, and Spring Boot Actuator health checks.

### Structured Logging
- **Logback + Logstash encoder** — all logs output as structured JSON to CloudWatch
- **Correlation ID filter** — every HTTP request gets a unique `requestId` (UUID) injected into MDC, printed on every log line, and returned in the `X-Request-ID` response header
- Enables end-to-end tracing of a single request through logs

### CloudWatch Log Groups
- Log group: `/ecs/dynamodb-crud-api`
- Retention: **14 days** (prevents unbounded storage costs)
- Driver: `awslogs` (configured in ECS task definition)

### CloudWatch Alarms

| Alarm | Metric | Threshold | Meaning |
|-------|--------|-----------|---------|
| `DLQ-Messages-Alert` | `ApproximateNumberOfMessagesVisible` on `item-variant-dlq` | ≥ 1 | A message failed processing 3 times — requires investigation |
| `ECS-Task-Failure-Alert` | `MemoryUtilization` on ECS service | > 90% | Container running out of memory — may crash soon |
| `API-Error-Alert` | `ErrorCount` (custom metric from log filter) | ≥ 5 in 60s | High error rate in application logs |

### How to Respond to Each Alarm

**DLQ-Messages-Alert**
- Check CloudWatch Logs for the failed message payload
- Look for errors in `item-variant-sqs-processor` Lambda logs
- Fix the root cause, then manually purge or reprocess the DLQ message

**ECS-Task-Failure-Alert**
- Check ECS task CloudWatch logs for OOM errors
- Consider increasing task memory in the task definition (`--memory 1024`)
- Check for memory leaks in recent deployments

**API-Error-Alert**
- Query CloudWatch Logs Insights: `filter @message like /ERROR/`
- Check for DynamoDB throttling, S3 errors, or application exceptions
- Roll back the last deployment if errors started after a push

### SNS Notifications
- Topic: `dynamodb-crud-alerts`
- Subscription: email to `ria_kashyap@perfaware.com`
- All 3 alarms publish to this topic

### CloudWatch Dashboard
Dashboard: `DynamodbCrudApi-Dashboard`

| Widget | Metrics shown |
|--------|--------------|
| SQS Queue Depth | `item-variant-queue` + `item-variant-dlq` message count |
| ECS CPU and Memory | CPU % + Memory % for `dynamodb-crud-service` |
| Application Error Count | Custom `ErrorCount` metric from log filter |
| Alarm Status | Live status of all 3 alarms |

### Health Checks
Spring Boot Actuator endpoint: `GET /actuator/health`

Custom health indicators:
- **DynamoDB** — calls `listTables` to verify connectivity
- **S3** — calls `headBucket` to verify bucket exists and is accessible
- **SQS** — calls `getQueueAttributes` to verify queue is reachable

ECS container health check runs every 30 seconds:
```
curl -f http://localhost:8080/actuator/health || exit 1
```
If the health check fails 3 times, ECS automatically replaces the task.

---

## AWS Infrastructure Summary

| Resource | Name/Value |
|----------|-----------|
| DynamoDB Table | `ItemVariants` |
| Lambda | `item-variant-api` |
| Lambda | `item-variant-sqs-processor` |
| Lambda | `item-variant-dlq-processor` |
| API Gateway | `https://cuoxxwnov4.execute-api.us-east-1.amazonaws.com` |
| SQS Queue | `item-variant-queue` |
| SQS DLQ | `item-variant-dlq` |
| S3 Bucket | `item-variant-details-846497880922` |
| Secrets Manager | `item-variant-api/config` |
| ECR Repository | `dynamodb-crud-api` |
| ECS Cluster | `dynamodb-crud-cluster` |
| ECS Service | `dynamodb-crud-service` |
| SNS Topic | `dynamodb-crud-alerts` |
| CloudWatch Dashboard | `DynamodbCrudApi-Dashboard` |
| CloudWatch Log Group | `/ecs/dynamodb-crud-api` (14 day retention) |
| Region | `us-east-1` |
| AWS Account | `846497880922` |

---

## Cost Note

| Service | Free Tier | How we stay within it |
|---------|-----------|----------------------|
| AWS Lambda | 1M requests/month free | Used for API, SQS consumer, DLQ processor |
| API Gateway | 1M calls/month free (HTTP API) | HTTP API v2 used |
| DynamoDB | 25GB storage + 25 RCU/WCU free | Small dataset, well within limits |
| S3 | 5GB storage free | Small JSON files only |
| SQS | 1M requests/month free | Low message volume |
| Secrets Manager | 30-day free trial per secret | 1 secret used |
| ECR | 500MB/month free | Lifecycle policy keeps max 3 images (~112MB each) |
| ECS Fargate | **Not free tier** | Service scaled to 0 tasks when not testing — $0 at rest |
| CloudWatch | 5GB log ingestion free, 10 alarms free | 3 alarms, minimal log volume |
| GitHub Actions | Free on public repos | Used instead of CodePipeline/CodeBuild |

**Estimated monthly cost: $0–$1**
