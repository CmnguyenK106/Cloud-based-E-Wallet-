---
title: "Resource cleanup"
date: 2024-01-01
weight: 7
chapter: false
pre: " <b> 5.7. </b> "
---

## Cleanup steps

Cleanup follows dependency order to prevent accidental data loss and avoid residual resources continuing to incur charges.

1. **Back up required data:** export required records and create a final Amazon RDS snapshot before deleting the database.
2. **Clean up Amazon CloudFront:** remove alternate domain names when necessary, disable the distribution, wait for deployment to complete, and delete it.
3. **Clean up Amazon S3:** delete all objects and object versions from the frontend bucket, then delete the bucket after CloudFront no longer uses it.
4. **Delete the Application Load Balancer:** remove its listener, the ALB, and the backend target group.
5. **Clean up Amazon EC2:** terminate the backend instance and review unused EBS volumes, snapshots, and Elastic IP addresses.
6. **Delete Amazon RDS:** confirm that the final snapshot exists, then delete the DB instance and unused DB subnet group.
7. **Delete network resources:** delete unreferenced security groups, followed by custom route tables, subnets, the Internet Gateway, and the VPC.
8. **Clean up IAM:** detach policies and delete IAM roles or users dedicated to the project; deactivate and delete unused SES SMTP credentials.
9. **Clean up Amazon SES and DNS:** remove the SES identity, DKIM, MAIL FROM, ACM validation, and Cloudflare DNS records when the domain is no longer used for the website or email.
10. **Review costs:** check AWS Billing and Cost Explorer to confirm that no unexpected resources continue to incur charges.

Before each deletion, the team verifies the resource name, Region, and dependencies. CloudFront must be disabled before deletion; a stopped EC2 instance can still incur EBS charges; snapshots, Elastic IP addresses, and S3 data can also be billed separately.