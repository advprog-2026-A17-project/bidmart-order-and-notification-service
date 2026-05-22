# Deployment Strategy

The order and notification service follows BidMart's staging-first progressive promotion strategy through Heroku.

## Environment Mapping

| Branch | Environment | Platform |
| --- | --- | --- |
| `staging` | Staging | Heroku staging app |
| `main` | Production | Heroku production app |

## Gate

Heroku automatic deployment should wait for GitHub CI checks before deploying. CI runs tests, coverage reporting, and SonarCloud analysis before the branch is considered deployable.

## Promotion Flow

1. Merge order/notification changes into `staging`.
2. CI validates the service.
3. Heroku deploys staging after required checks pass.
4. Validate order creation, notification delivery, and gateway integration.
5. Promote the same change to `main`.
6. Heroku deploys production after required checks pass.

## Rollback

Rollback uses Heroku release rollback for urgent incidents, or a Git revert on `main` followed by a normal CI-gated deploy.
