COMPOSE := docker compose
.DEFAULT_GOAL := dev

.PHONY: dev staging prod down logs build-staging build-prod clean ps ps-dev ps-staging ps-prod test format format-check

dev:
	$(COMPOSE) up --build dev

staging:
	$(COMPOSE) --env-file .env.staging -f compose.staging.yaml up --build -d staging

prod:
	$(COMPOSE) --env-file .env.prod -f compose.prod.yaml up --build -d prod

build-staging:
	$(COMPOSE) --env-file .env.staging -f compose.staging.yaml build staging

build-prod:
	$(COMPOSE) --env-file .env.prod -f compose.prod.yaml build prod

down:
	$(COMPOSE) down --remove-orphans
	DB_NAME=unused DB_USER=unused DB_PASSWORD=unused JWT_SECRET=unused $(COMPOSE) -f compose.staging.yaml down --remove-orphans
	DB_NAME=unused DB_USER=unused DB_PASSWORD=unused JWT_SECRET=unused $(COMPOSE) -f compose.prod.yaml down --remove-orphans

logs:
	$(COMPOSE) logs -f dev

clean:
	$(COMPOSE) down --volumes --remove-orphans
	DB_NAME=unused DB_USER=unused DB_PASSWORD=unused JWT_SECRET=unused $(COMPOSE) -f compose.staging.yaml down --volumes --remove-orphans
	DB_NAME=unused DB_USER=unused DB_PASSWORD=unused JWT_SECRET=unused $(COMPOSE) -f compose.prod.yaml down --volumes --remove-orphans

ps:
	$(COMPOSE) ps

ps-dev:
	$(COMPOSE) ps

ps-staging:
	$(COMPOSE) --env-file .env.staging -f compose.staging.yaml ps

ps-prod:
	$(COMPOSE) --env-file .env.prod -f compose.prod.yaml ps

test:
	./gradlew --no-daemon test

format:
	./gradlew spotlessApply

format-check:
	./gradlew spotlessCheck
