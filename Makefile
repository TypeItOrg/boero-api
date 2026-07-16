COMPOSE := docker compose
DEV_CONTAINERS := boero-api-dev boero-api-postgres-dev boero-api-redis-dev
.DEFAULT_GOAL := dev

MIGRATION_NAME := $(word 2,$(MAKECMDGOALS))
KNOWN_TARGETS := dev staging prod repair-staging down logs logs-dev logs-staging logs-prod clean ps ps-dev ps-staging ps-prod test format format-check migration

ifeq ($(firstword $(MAKECMDGOALS)),migration)
ifneq ($(MIGRATION_NAME),)
ifneq ($(filter $(MIGRATION_NAME),$(KNOWN_TARGETS)),)
$(error Migration name '$(MIGRATION_NAME)' conflicts with a Make target)
endif
.PHONY: $(MIGRATION_NAME)
$(MIGRATION_NAME):
	@:
endif
endif

.PHONY: dev staging prod repair-staging down logs logs-dev logs-staging logs-prod clean ps ps-dev ps-staging ps-prod test format format-check migration

dev:
	$(COMPOSE) up --build dev

staging:
	$(COMPOSE) --env-file .env.staging -f compose.staging.yaml pull staging
	$(COMPOSE) --env-file .env.staging -f compose.staging.yaml up -d staging

prod:
	$(COMPOSE) --env-file .env.prod -f compose.prod.yaml pull prod
	$(COMPOSE) --env-file .env.prod -f compose.prod.yaml up -d prod

repair-staging:
	docker rm -f $(DEV_CONTAINERS) 2>/dev/null || true
	docker network rm boero-api-network-dev 2>/dev/null || true
	$(MAKE) staging

down:
	$(COMPOSE) down --remove-orphans
	APP_VERSION=unused DB_NAME=unused DB_USER=unused DB_PASSWORD=unused JWT_SECRET=unused $(COMPOSE) -f compose.staging.yaml down --remove-orphans
	APP_VERSION=unused DB_NAME=unused DB_USER=unused DB_PASSWORD=unused JWT_SECRET=unused $(COMPOSE) -f compose.prod.yaml down --remove-orphans

logs: logs-dev

logs-dev:
	$(COMPOSE) logs -f dev

logs-staging:
	$(COMPOSE) --env-file .env.staging -f compose.staging.yaml logs -f staging

logs-prod:
	$(COMPOSE) --env-file .env.prod -f compose.prod.yaml logs -f prod

clean:
	$(COMPOSE) down --volumes --remove-orphans
	APP_VERSION=unused DB_NAME=unused DB_USER=unused DB_PASSWORD=unused JWT_SECRET=unused $(COMPOSE) -f compose.staging.yaml down --volumes --remove-orphans
	APP_VERSION=unused DB_NAME=unused DB_USER=unused DB_PASSWORD=unused JWT_SECRET=unused $(COMPOSE) -f compose.prod.yaml down --volumes --remove-orphans

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

migration:
	@name="$(MIGRATION_NAME)"; \
	case "$$name" in \
		"") echo "Usage: make migration add_description_to_users_table" >&2; exit 1 ;; \
		[a-z]*) ;; \
		*) echo "Migration name must start with a lowercase letter" >&2; exit 1 ;; \
	esac; \
	case "$$name" in \
		*[!a-z0-9_]*) echo "Migration name must use lowercase snake_case" >&2; exit 1 ;; \
	esac; \
	timestamp=$$(date -u +%Y%m%d%H%M%S); \
	path="src/main/resources/db/migration/$${timestamp}__$${name}.sql"; \
	if [ -e "$$path" ]; then echo "Migration already exists: $$path" >&2; exit 1; fi; \
	touch "$$path"; \
	echo "Created $$path"
