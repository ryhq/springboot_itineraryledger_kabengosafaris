#!/bin/bash

################################################################################
# Kabenga Safaris Restore Script
#
# This script restores backups created by backup.sh
#
# Usage:
#   ./restore.sh BACKUP_PATH [OPTIONS]
#
# Options:
#   --db-only       Restore database only
#   --files-only    Restore files only
#   --help          Show this help message
#
# Example:
#   ./restore.sh /opt/lampp/htdocs/kabengosafaris/backups/kabengosafaris_backup_20260209_020000.zip
#
# Author: Kabenga Safaris IT Team
# Date: 2026-02-09
################################################################################

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-springboot_itineraryledger_kabengosafaris}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"

FILES_BASE_PATH="${FILES_BASE_PATH:-/opt/lampp/htdocs/kabengosafaris/ItineraryLedger}"

# Script options
RESTORE_DB=true
RESTORE_FILES=true

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Show help
show_help() {
    head -n 18 "$0" | tail -n 13
    exit 0
}

# Parse arguments
if [ $# -eq 0 ]; then
    echo -e "${RED}Error: No backup path specified${NC}"
    show_help
fi

BACKUP_PATH="$1"
shift

while [[ $# -gt 0 ]]; do
    case $1 in
        --db-only)
            RESTORE_FILES=false
            shift
            ;;
        --files-only)
            RESTORE_DB=false
            shift
            ;;
        --help)
            show_help
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            show_help
            ;;
    esac
done

# Validate backup path
if [ ! -e "$BACKUP_PATH" ]; then
    echo -e "${RED}Error: Backup path does not exist: $BACKUP_PATH${NC}"
    exit 1
fi

# Print banner
echo -e "${BLUE}╔════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                                                                    ║${NC}"
echo -e "${BLUE}║              KABENGA SAFARIS RESTORE SCRIPT                        ║${NC}"
echo -e "${BLUE}║                                                                    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "Restore started at: $(date)"
echo "Backup source: $BACKUP_PATH"
echo ""

# Confirmation
echo -e "${YELLOW}WARNING: This will overwrite existing data!${NC}"
echo -e "${YELLOW}Database: ${DB_NAME}${NC}"
echo -e "${YELLOW}Files: ${FILES_BASE_PATH}${NC}"
echo ""
read -p "Are you sure you want to continue? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo -e "${RED}Restore cancelled by user${NC}"
    exit 0
fi

# Create temporary extraction directory
TEMP_DIR="/tmp/kabenga_restore_$$"
mkdir -p "$TEMP_DIR"

# Extract backup
echo -e "${YELLOW}Extracting backup...${NC}"

if [[ "$BACKUP_PATH" == *.zip ]]; then
    unzip -q "$BACKUP_PATH" -d "$TEMP_DIR"
elif [[ "$BACKUP_PATH" == *.tar.gz ]]; then
    tar -xzf "$BACKUP_PATH" -C "$TEMP_DIR"
elif [ -d "$BACKUP_PATH" ]; then
    cp -r "$BACKUP_PATH"/* "$TEMP_DIR/"
else
    echo -e "${RED}Error: Unknown backup format${NC}"
    rm -rf "$TEMP_DIR"
    exit 1
fi

echo -e "${GREEN}✓ Backup extracted${NC}"

# Find backup directory
BACKUP_DIR=$(find "$TEMP_DIR" -maxdepth 1 -type d -name "*backup*" | head -1)
if [ -z "$BACKUP_DIR" ]; then
    BACKUP_DIR="$TEMP_DIR"
fi

# Restore database
if [ "$RESTORE_DB" = true ]; then
    echo -e "${YELLOW}[1/2] Restoring database...${NC}"

    SQL_FILE=$(find "$BACKUP_DIR" -name "database_*.sql" | head -1)

    if [ -n "$SQL_FILE" ]; then
        RESTORE_CMD="mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER}"

        if [ -n "$DB_PASSWORD" ]; then
            RESTORE_CMD="$RESTORE_CMD -p${DB_PASSWORD}"
        fi

        RESTORE_CMD="$RESTORE_CMD ${DB_NAME} < ${SQL_FILE}"

        if eval $RESTORE_CMD; then
            echo -e "${GREEN}✓ Database restored successfully${NC}"
        else
            echo -e "${RED}✗ Database restore failed${NC}"
            rm -rf "$TEMP_DIR"
            exit 1
        fi
    else
        echo -e "${YELLOW}⊘ No database backup found${NC}"
    fi
fi

# Restore files
if [ "$RESTORE_FILES" = true ]; then
    echo -e "${YELLOW}[2/2] Restoring files...${NC}"

    FILES_DIR="${BACKUP_DIR}/files"

    if [ -d "$FILES_DIR" ]; then
        # Create backup of current files
        CURRENT_BACKUP="${FILES_BASE_PATH}_backup_$(date +%Y%m%d_%H%M%S)"
        echo "  Creating backup of current files: $CURRENT_BACKUP"
        cp -r "$FILES_BASE_PATH" "$CURRENT_BACKUP"

        # Restore files
        for dir in "$FILES_DIR"/*; do
            if [ -d "$dir" ]; then
                DIR_NAME=$(basename "$dir")
                TARGET="${FILES_BASE_PATH}/${DIR_NAME}"

                # Remove existing directory
                if [ -d "$TARGET" ]; then
                    rm -rf "$TARGET"
                fi

                # Copy restored directory
                if cp -r "$dir" "$TARGET"; then
                    echo -e "  ${GREEN}✓${NC} $DIR_NAME"
                else
                    echo -e "  ${RED}✗${NC} $DIR_NAME"
                fi
            fi
        done

        echo -e "${GREEN}✓ Files restored successfully${NC}"
    else
        echo -e "${YELLOW}⊘ No file backup found${NC}"
    fi
fi

# Cleanup
rm -rf "$TEMP_DIR"

# Success banner
echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                                                                    ║${NC}"
echo -e "${GREEN}║            RESTORE PROCESS COMPLETED SUCCESSFULLY                  ║${NC}"
echo -e "${GREEN}║                                                                    ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "Restore completed at: $(date)"
