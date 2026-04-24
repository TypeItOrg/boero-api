COMPOSE := docker compose

.PHONY: dev dev-build staging staging-build prod prod-build down logs ps test

dev:
	$(COMPOSE) --env-file .env.dev -f compose.yaml -f compose.dev.yaml up

dev-build:
	$(COMPOSE) --env-file .env.dev -f compose.yaml -f compose.dev.yaml up --build

staging:
	$(COMPOSE) --env-file .env.staging -f compose.yaml -f compose.staging.yaml up -d

staging-build:
	$(COMPOSE) --env-file .env.staging -f compose.yaml -f compose.staging.yaml up --build -d

prod:
	$(COMPOSE) --env-file .env.prod -f compose.yaml -f compose.prod.yaml up -d

prod-build:
	$(COMPOSE) --env-file .env.prod -f compose.yaml -f compose.prod.yaml up --build -d

down:
	$(COMPOSE) -f compose.yaml -f compose.dev.yaml -f compose.staging.yaml -f compose.prod.yaml down --remove-orphans

logs:
	$(COMPOSE) -f compose.yaml -f compose.dev.yaml logs -f

ps:
	$(COMPOSE) -f compose.yaml -f compose.dev.yaml ps

test:
	./gradlew --no-daemon test
