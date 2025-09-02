# MakeFile

Available commands:
- make help - Shows usage instructions and available targets
- make create-schema - Creates the account_management schema
- make drop-schema - Drops the schema with confirmation prompt
- make recreate-schema - Drops and recreates the schema
- make check-connection - Tests database connectivity

Usage:
cd jbh-account
make -f Account.mk help
make -f Account.mk create-schema
make -f Account.mk drop-schema
