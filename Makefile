COMPOSE := docker compose
.DEFAULT_GOAL := dev

MIGRATION_NAME := $(word 2,$(MAKECMDGOALS))
KNOWN_TARGETS := dev build down logs clean reset-data ps test format format-check migration

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

dev:
	$(COMPOSE) watch

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
