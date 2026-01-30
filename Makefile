# JBH Personal Finance - Multi-Module Database Management
# Root Makefile for managing all module schemas

# Module directories
ACCOUNT_DIR = jbh-account
NOTIFICATION_DIR = jbh-notification
PREFERENCES_DIR = jbh-preferences

# Colors for help output
GREEN = \033[0;32m
YELLOW = \033[1;33m
BLUE = \033[0;34m
RED = \033[0;31m
NC = \033[0m # No Color

.PHONY: help account-help notification-help preferences-help
.PHONY: create-all-schemas drop-all-schemas recreate-all-schemas check-all-connections
.PHONY: export-all-schemas export-account-schema export-notification-schema export-preferences-schema
.PHONY: account-create-schema account-drop-schema account-recreate-schema account-check-connection
.PHONY: notification-create-schema notification-drop-schema notification-recreate-schema notification-check-connection
.PHONY: preferences-create-schema preferences-drop-schema preferences-recreate-schema preferences-check-connection

help: ## Show this help message
	@echo "${GREEN}JBH Personal Finance - Multi-Module Database Management${NC}"
	@echo ""
	@echo "${YELLOW}Global Commands:${NC}"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  ${GREEN}%-25s${NC} %s\n", $$1, $$2}' $(MAKEFILE_LIST) | head -10
	@echo ""
	@echo "${YELLOW}Module-Specific Commands:${NC}"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  ${GREEN}%-25s${NC} %s\n", $$1, $$2}' $(MAKEFILE_LIST) | tail -n +11
	@echo ""
	@echo "${BLUE}Usage Examples:${NC}"
	@echo "  make help                     # Show this help"
	@echo "  make account-help             # Show account module help"
	@echo "  make create-all-schemas       # Create all schemas"
	@echo "  make account-create-schema    # Create only account schema"

# Global operations
create-all-schemas: ## Create all module schemas
	@echo "${GREEN}Creating all schemas...${NC}"
	@cd $(ACCOUNT_DIR) && $(MAKE) -f Account.mk create-schema
	@cd $(NOTIFICATION_DIR) && $(MAKE) -f Notification.mk create-schema
	@cd $(PREFERENCES_DIR) && $(MAKE) -f Preferences.mk create-schema
	@echo "${GREEN}All schemas created successfully.${NC}"

drop-all-schemas: ## Drop all module schemas (WARNING: Deletes all data)
	@echo "${RED}WARNING: This will delete ALL data in ALL schemas.${NC}"
	@read -p "Are you sure you want to continue? (y/N): " confirm && \
		if [ "$$confirm" = "y" ] || [ "$$confirm" = "Y" ]; then \
			echo "${GREEN}Dropping all schemas...${NC}"; \
			(cd $(ACCOUNT_DIR) && $(MAKE) -f Account.mk drop-schema); \
			(cd $(NOTIFICATION_DIR) && $(MAKE) -f Notification.mk drop-schema); \
			(cd $(PREFERENCES_DIR) && $(MAKE) -f Preferences.mk drop-schema); \
			echo "${GREEN}All schemas dropped successfully.${NC}"; \
		else \
			echo "Operation cancelled."; \
		fi

recreate-all-schemas: drop-all-schemas create-all-schemas ## Drop and recreate all schemas

check-all-connections: ## Test all database connections
	@echo "${GREEN}Testing all database connections...${NC}"
	@cd $(ACCOUNT_DIR) && $(MAKE) -f Account.mk check-connection
	@cd $(NOTIFICATION_DIR) && $(MAKE) -f Notification.mk check-connection
	@cd $(PREFERENCES_DIR) && $(MAKE) -f Preferences.mk check-connection

# Module help commands
account-help: ## Show account module help
	@echo "${BLUE}Account Module Commands:${NC}"
	@cd $(ACCOUNT_DIR) && $(MAKE) -f Account.mk help

notification-help: ## Show notification module help
	@echo "${BLUE}Notification Module Commands:${NC}"
	@cd $(NOTIFICATION_DIR) && $(MAKE) -f Notification.mk help

preferences-help: ## Show preferences module help
	@echo "${BLUE}Preferences Module Commands:${NC}"
	@cd $(PREFERENCES_DIR) && $(MAKE) -f Preferences.mk help

# Account module commands
account-create-schema: ## Create account module schema
	@cd $(ACCOUNT_DIR) && $(MAKE) -f Account.mk create-schema

account-drop-schema: ## Drop account module schema
	@cd $(ACCOUNT_DIR) && $(MAKE) -f Account.mk drop-schema

account-recreate-schema: ## Recreate account module schema
	@cd $(ACCOUNT_DIR) && $(MAKE) -f Account.mk recreate-schema

account-check-connection: ## Test account database connection
	@cd $(ACCOUNT_DIR) && $(MAKE) -f Account.mk check-connection

# Notification module commands
notification-create-schema: ## Create notification module schema
	@cd $(NOTIFICATION_DIR) && $(MAKE) -f Notification.mk create-schema

notification-drop-schema: ## Drop notification module schema
	@cd $(NOTIFICATION_DIR) && $(MAKE) -f Notification.mk drop-schema

notification-recreate-schema: ## Recreate notification module schema
	@cd $(NOTIFICATION_DIR) && $(MAKE) -f Notification.mk recreate-schema

notification-check-connection: ## Test notification database connection
	@cd $(NOTIFICATION_DIR) && $(MAKE) -f Notification.mk check-connection

# Preferences module commands
preferences-create-schema: ## Create preferences module schema
	@cd $(PREFERENCES_DIR) && $(MAKE) -f Preferences.mk create-schema

preferences-drop-schema: ## Drop preferences module schema
	@cd $(PREFERENCES_DIR) && $(MAKE) -f Preferences.mk drop-schema

preferences-recreate-schema: ## Recreate preferences module schema
	@cd $(PREFERENCES_DIR) && $(MAKE) -f Preferences.mk recreate-schema

preferences-check-connection: ## Test preferences database connection
	@cd $(PREFERENCES_DIR) && $(MAKE) -f Preferences.mk check-connection

# Schema export commands
export-all-schemas: ## Export all database schemas to docs/database-schemas
	@echo "${GREEN}Exporting all database schemas...${NC}"
	@./scripts/export-schema.sh all

export-account-schema: ## Export account schema to docs/database-schemas
	@echo "${GREEN}Exporting account schema...${NC}"
	@./scripts/export-schema.sh acctmgmt

export-notification-schema: ## Export notification schema to docs/database-schemas
	@echo "${GREEN}Exporting notification schema...${NC}"
	@./scripts/export-schema.sh notification

export-preferences-schema: ## Export preferences schema to docs/database-schemas
	@echo "${GREEN}Exporting preferences schema...${NC}"
	@./scripts/export-schema.sh userprefs