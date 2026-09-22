COMPOSE := docker compose
.DEFAULT_GOAL := dev

MIGRATION_NAME := $(word 2,$(MAKECMDGOALS))
KNOWN_TARGETS := dev build down logs clean reset-data ps test format format-check migration seed-demo seed-demo-repair-ids

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

.PHONY: dev build down logs clean reset-data ps test format format-check migration

.PHONY: seed-demo seed-demo-repair-ids
seed-demo:
	cat scripts/seed/enrollment-demo-ids.sql scripts/seed/enrollment-demo-catalog.sql scripts/seed/enrollment-demo.sql | $(COMPOSE) exec -T postgres sh -c 'exec psql -X -v ON_ERROR_STOP=1 -U "$$POSTGRES_USER" -d "$$POSTGRES_DB"'

seed-demo-repair-ids:
	cat scripts/seed/enrollment-demo-ids.sql scripts/seed/enrollment-demo-catalog.sql scripts/seed/enrollment-demo-repair-ids.sql | $(COMPOSE) exec -T postgres sh -c 'exec psql -X -v ON_ERROR_STOP=1 -U "$$POSTGRES_USER" -d "$$POSTGRES_DB"'

dev:
	@$(COMPOSE) rm --stop --force dev
	@$(COMPOSE) up --build --watch --remove-orphans

build:
	$(COMPOSE) build

down:
	$(COMPOSE) down --remove-orphans

logs:
	$(COMPOSE) logs -f dev

clean:
	./gradlew clean

reset-data:
	$(COMPOSE) down --volumes --remove-orphans

ps:
	$(COMPOSE) ps

test:
	./gradlew --no-daemon test

format:
	./gradlew spotlessApply

format-check:
	./gradlew spotlessCheck

ifeq ($(OS),Windows_NT)
migration:
	@powershell -NoProfile -Command "$$name = '$(MIGRATION_NAME)'; if ([string]::IsNullOrWhiteSpace($$name)) { Write-Error 'Usage: make migration add_description_to_users_table'; exit 1 }; if ($$name -notmatch '^[a-z][a-z0-9_]*$$') { Write-Error 'Migration name must use lowercase snake_case and start with a lowercase letter'; exit 1 }; $$timestamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss'); $$path = 'src/main/resources/db/migration/' + $$timestamp + '__' + $$name + '.sql'; if (Test-Path $$path) { Write-Error ('Migration already exists: ' + $$path); exit 1 }; New-Item -ItemType File -Path $$path | Out-Null; Write-Output ('Created ' + $$path)"
else
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
endif
