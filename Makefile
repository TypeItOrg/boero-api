COMPOSE := docker compose

.PHONY: dev dev-build staging staging-build prod prod-build down down-dev down-staging down-prod logs logs-dev logs-staging logs-prod ps ps-dev ps-staging ps-prod test

dev:
	$(COMPOSE) -f compose.yaml -f compose.dev.yaml up

dev-build:
	$(COMPOSE) -f compose.yaml -f compose.dev.yaml up --build

staging:
	$(COMPOSE) --env-file .env.staging -f compose.yaml -f compose.staging.yaml up -d

staging-build:
	$(COMPOSE) --env-file .env.staging -f compose.yaml -f compose.staging.yaml up --build -d

prod:
	$(COMPOSE) --env-file .env.prod -f compose.yaml -f compose.prod.yaml up -d

prod-build:
	$(COMPOSE) --env-file .env.prod -f compose.yaml -f compose.prod.yaml up --build -d

down:
	$(COMPOSE) -f compose.yaml -f compose.dev.yaml down --remove-orphans

down-dev:
	$(COMPOSE) -f compose.yaml -f compose.dev.yaml down --remove-orphans

down-staging:
	$(COMPOSE) --env-file .env.staging -f compose.yaml -f compose.staging.yaml down --remove-orphans

down-prod:
	$(COMPOSE) --env-file .env.prod -f compose.yaml -f compose.prod.yaml down --remove-orphans

logs:
	$(COMPOSE) -f compose.yaml -f compose.dev.yaml logs -f

logs-dev:
	$(COMPOSE) -f compose.yaml -f compose.dev.yaml logs -f

logs-staging:
	$(COMPOSE) --env-file .env.staging -f compose.yaml -f compose.staging.yaml logs -f

logs-prod:
	$(COMPOSE) --env-file .env.prod -f compose.yaml -f compose.prod.yaml logs -f

ps:
	$(COMPOSE) -f compose.yaml -f compose.dev.yaml ps

ps-dev:
	$(COMPOSE) -f compose.yaml -f compose.dev.yaml ps

ps-staging:
	$(COMPOSE) --env-file .env.staging -f compose.yaml -f compose.staging.yaml ps

ps-prod:
	$(COMPOSE) --env-file .env.prod -f compose.yaml -f compose.prod.yaml ps

test:
	./gradlew --no-daemon test
