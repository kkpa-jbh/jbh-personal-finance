#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Database connection parameters
DB_HOST="${DATABASE_HOST:-localhost}"
DB_PORT="${DATABASE_PORT:-5432}"
DB_NAME="${DATABASE_NAME:-jbh_finance}"
DB_USER="${DATABASE_USERNAME:-jbh_admin}"
DB_PASSWORD="${DATABASE_PASSWORD:-raspukk}"

# Default to all schemas if none specified
SCHEMA="${1:-all}"
OUTPUT_DIR="${2:-docs/database-schemas}"

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Function to export a single schema
export_schema() {
    local schema_name=$1
    local output_file="$OUTPUT_DIR/${schema_name}-schema.sql"

    echo -e "${YELLOW}Exporting schema: ${schema_name}${NC}"

    PGPASSWORD="$DB_PASSWORD" pg_dump \
        -h "$DB_HOST" \
        -p "$DB_PORT" \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --schema="$schema_name" \
        --schema-only \
        --no-owner \
        --no-privileges \
        -f "$output_file"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Schema exported to: ${output_file}${NC}"

        # Add metadata header
        temp_file="${output_file}.tmp"
        {
            echo "-- ============================================="
            echo "-- JBH Personal Finance - Database Schema"
            echo "-- Schema: ${schema_name}"
            echo "-- Database: ${DB_NAME}"
            echo "-- Exported: $(date '+%Y-%m-%d %H:%M:%S')"
            echo "-- ============================================="
            echo ""
            cat "$output_file"
        } > "$temp_file"
        mv "$temp_file" "$output_file"

        # Show file size
        size=$(du -h "$output_file" | cut -f1)
        echo -e "${GREEN}  File size: ${size}${NC}"
    else
        echo -e "${RED}✗ Failed to export schema: ${schema_name}${NC}"
        return 1
    fi
}

# Main execution
case "$SCHEMA" in
    "all")
        echo -e "${GREEN}Exporting all schemas...${NC}"
        export_schema "finance"
        export_schema "notification"
        export_schema "userprefs"

        # Create combined schema file with existing schemas only
        combined_file="$OUTPUT_DIR/all-schemas.sql"
        {
            echo "-- ============================================="
            echo "-- JBH Personal Finance - All Schemas"
            echo "-- Database: ${DB_NAME}"
            echo "-- Exported: $(date '+%Y-%m-%d %H:%M:%S')"
            echo "-- ============================================="
            echo ""
            for schema_file in "$OUTPUT_DIR"/*-schema.sql; do
                if [ -f "$schema_file" ] && [ "$schema_file" != "$combined_file" ]; then
                    cat "$schema_file"
                    echo ""
                fi
            done
        } > "$combined_file"
        echo -e "${GREEN}✓ Combined schema exported to: ${combined_file}${NC}"
        ;;
    "finance"|"notification"|"userprefs")
        export_schema "$SCHEMA"
        ;;
    *)
        echo -e "${RED}Unknown schema: ${SCHEMA}${NC}"
        echo "Usage: $0 [all|finance|notification|userprefs] [output_dir]"
        exit 1
        ;;
esac

echo -e "${GREEN}Schema export completed!${NC}"