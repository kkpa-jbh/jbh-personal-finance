# Database Configuration
DB_NAME = jbh_finance
DB_USER = jbh_admin
DB_HOST = localhost
DB_PORT = 5432
SCHEMA_NAME = finance

# Colors for help output
GREEN = \033[0;32m
YELLOW = \033[1;33m
NC = \033[0m # No Color

.PHONY: help create-schema drop-schema recreate-schema

help: ## Show this help message
	@echo "${GREEN}JBH Products Database Schema Management${NC}"
	@echo ""
	@echo "${YELLOW}Available targets:${NC}"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  ${GREEN}%-15s${NC} %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo ""
	@echo "${YELLOW}Database Configuration:${NC}"
	@echo "  Host: ${DB_HOST}:${DB_PORT}"
	@echo "  Database: ${DB_NAME}"
	@echo "  User: ${DB_USER}"
	@echo "  Schema: ${SCHEMA_NAME}"

create-schema: ## Create the database schema
	@echo "${GREEN}Creating schema '${SCHEMA_NAME}' in database '${DB_NAME}'...${NC}"
	@psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -c "CREATE SCHEMA IF NOT EXISTS ${SCHEMA_NAME};" && \
		echo "${GREEN}Schema '${SCHEMA_NAME}' created successfully.${NC}" || \
		echo "Failed to create schema '${SCHEMA_NAME}'."

drop-schema: ## Drop the database schema (WARNING: This will delete all data)
	@echo "${YELLOW}WARNING: This will delete all data in schema '${SCHEMA_NAME}'.${NC}"
	@read -p "Are you sure you want to continue? (y/N): " confirm && \
		if [ "$$confirm" = "y" ] || [ "$$confirm" = "Y" ]; then \
			psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -c "DROP SCHEMA IF EXISTS ${SCHEMA_NAME} CASCADE;" && \
				echo "${GREEN}Schema '${SCHEMA_NAME}' dropped successfully.${NC}" || \
				echo "Failed to drop schema '${SCHEMA_NAME}'."; \
		else \
			echo "Operation cancelled."; \
		fi

recreate-schema: drop-schema create-schema ## Drop and recreate the database schema

check-connection: ## Test database connection
	@echo "${GREEN}Testing connection to database...${NC}"
	@psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -c "SELECT current_database(), current_schema(), current_user;" && \
		echo "${GREEN}Connection successful.${NC}" || \
		echo "Connection failed."
