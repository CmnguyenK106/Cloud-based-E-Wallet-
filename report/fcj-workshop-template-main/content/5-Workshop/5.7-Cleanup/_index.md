---
title: "Resource cleanup"
date: 2024-01-01
weight: 7
chapter: false
pre: " <b> 5.7. </b> "
---

## Objective

Avoid post-demo cost without deleting required data or active resources. Never use bulk deletion with unverified IDs.

## Cleanup order

1. Back up the database and required evidence.
2. If unused, disable and delete CloudFront.
3. Delete verified S3 objects and bucket.
4. Delete ALB, listener, and target group.
5. Stop or terminate EC2. `stop` does not delete EBS; review EBS, snapshots, and Elastic IP separately.
6. Delete RDS after deciding on a final snapshot; deletion may destroy data.
7. Delete security groups after dependencies are gone.
8. Delete subnets/VPC only after all network interfaces/resources are gone.
9. Remove Cloudflare/SES verification and DKIM records, the SES identity, or SMTP credentials only when the domain and application no longer use email.
10. Review AWS Billing/Cost Explorer after cleanup.

> **Image required:** Resource inventory and Billing before/after cleanup.

<!-- IMAGE_PATH: /images/5-Workshop/5.7-Cleanup/resource-inventory.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.7-Cleanup/billing-before.png -->
<!-- IMAGE_PATH: /images/5-Workshop/5.7-Cleanup/billing-after.png -->

## Warnings

CloudFront may take time to disable before deletion. Do not delete a Cloudflare zone used by another domain/email purpose. Snapshots, EBS, Elastic IP, and S3 can incur separate costs.
