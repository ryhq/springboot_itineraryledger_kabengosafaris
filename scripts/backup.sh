#!/bin/bash

################################################################################
# Kabenga Safaris Backup Script
#
# This script performs comprehensive backups of:
# - MySQL database
# - File storage directories (images, documents, templates)
#
# Features:
# - Automatic timestamp generation
# - Compression (ZIP or TAR.GZ)
# - Retention management (delete old backups)
# - Logging
# - Email notifications (optional)
#
# Usage:
#   ./backup.sh [OPTIONS]
#
# Options:
#   --db-only       Backup database only
#   --files-only    Backup files only
#   --no-compress   Skip compression
#   --no-cleanup    Skip old backup cleanup
#   --help          Show this help message
#
# Author: Kabenga Safaris IT Team
# Date: 2026-02-09
################################################################################

# Configuration (modify these as needed)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-springboot_itineraryledger_kabengosafaris}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"

FILES_BASE_PATH="${FILES_BASE_PATH:-./data/}"
BACKUP_BASE_PATH="${BACKUP_BASE_PATH:-/opt/lampp/htdocs/kabengosafaris/backups}"

RETENTION_DAYS="${RETENTION_DAYS:-7}"
RETENTION_MAX_COUNT="${RETENTION_MAX_COUNT:-30}"

NOTIFICATION_EMAIL="${NOTIFICATION_EMAIL:-admin@kabengosafaris.com}"
NOTIFY_ON_SUCCESS="${NOTIFY_ON_SUCCESS:-false}"
NOTIFY_ON_FAILURE="${NOTIFY_ON_FAILURE:-true}"

# Script options
BACKUP_DB=true
BACKUP_FILES=true
COMPRESS=true
CLEANUP=true

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --db-only)
            BACKUP_FILES=false
            shift
            ;;
        --files-only)
            BACKUP_DB=false
            shift
            ;;
        --no-compress)
            COMPRESS=false
            shift
            ;;
        --no-cleanup)
            CLEANUP=false
            shift
            ;;
        --help)
            head -n 31 "$0" | tail -n 26
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Generate timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_NAME="kabengosafaris_backup_${TIMESTAMP}"
TEMP_BACKUP_DIR="${BACKUP_BASE_PATH}/${BACKUP_NAME}_temp"
LOG_FILE="${BACKUP_BASE_PATH}/backup_${TIMESTAMP}.log"

# Create necessary directories
mkdir -p "${BACKUP_BASE_PATH}"
mkdir -p "${TEMP_BACKUP_DIR}"

# Logging function
log() {
    echo -e "$1" | tee -a "${LOG_FILE}"
}

# Print banner
print_banner() {
    log "${BLUE}╔════════════════════════════════════════════════════════════════════╗${NC}"
    log "${BLUE}║                                                                    ║${NC}"
    log "${BLUE}║              KABENGA SAFARIS BACKUP SCRIPT                         ║${NC}"
    log "${BLUE}║                                                                    ║${NC}"
    log "${BLUE}╚════════════════════════════════════════════════════════════════════╝${NC}"
    log ""
    log "Backup started at: $(date)"
    log "Backup name: ${BACKUP_NAME}"
    log ""
}

# Backup database
backup_database() {
    log "${YELLOW}[1/2] Backing up database...${NC}"

    DB_BACKUP_FILE="${TEMP_BACKUP_DIR}/database_${TIMESTAMP}.sql"

    # Build mysqldump command
    DUMP_CMD="mysqldump -h${DB_HOST} -P${DB_PORT} -u${DB_USER}"

    if [ -n "$DB_PASSWORD" ]; then
        DUMP_CMD="$DUMP_CMD -p${DB_PASSWORD}"
    fi

    DUMP_CMD="$DUMP_CMD --routines --triggers --events --single-transaction --quick --lock-tables=false"
    DUMP_CMD="$DUMP_CMD ${DB_NAME} --result-file=${DB_BACKUP_FILE}"

    # Execute backup
    if eval $DUMP_CMD 2>&1 | tee -a "${LOG_FILE}"; then
        DB_SIZE=$(du -h "${DB_BACKUP_FILE}" | cut -f1)
        log "${GREEN}✓ Database backup completed successfully (${DB_SIZE})${NC}"
        return 0
    else
        log "${RED}✗ Database backup failed${NC}"
        return 1
    fi
}

# Backup files
backup_files() {
    log "${YELLOW}[2/2] Backing up file directories...${NC}"

    FILES_BACKUP_DIR="${TEMP_BACKUP_DIR}/files"
    mkdir -p "${FILES_BACKUP_DIR}"

    # List of directories to backup
    DIRS=(
        "email-signatures"
        "email-templates"
        "pdf-templates"
        "accommodation-images"
        "accommodation-documents"
        "park-images"
        "park-documents"
        "activity-images"
        "activity-documents"
        "park-activity-images"
        "park-activity-documents"
        "itinerary-documents"
        "quote-documents"
        "safari-documents"
    )

    SUCCESS_COUNT=0
    TOTAL_COUNT=${#DIRS[@]}

    for dir in "${DIRS[@]}"; do
        SOURCE="${FILES_BASE_PATH}/${dir}"
        TARGET="${FILES_BACKUP_DIR}/${dir}"

        if [ -d "$SOURCE" ]; then
            if cp -r "$SOURCE" "$TARGET" 2>&1 | tee -a "${LOG_FILE}"; then
                DIR_SIZE=$(du -sh "$TARGET" | cut -f1)
                log "  ${GREEN}✓${NC} ${dir} (${DIR_SIZE})"
                ((SUCCESS_COUNT++))
            else
                log "  ${RED}✗${NC} ${dir} (failed)"
            fi
        else
            log "  ${YELLOW}⊘${NC} ${dir} (not found, skipping)"
        fi
    done

    log "${GREEN}✓ File backup completed: ${SUCCESS_COUNT}/${TOTAL_COUNT} directories backed up${NC}"

    if [ $SUCCESS_COUNT -eq 0 ]; then
        return 1
    fi

    return 0
}

# Compress backup
compress_backup() {
    log "${YELLOW}Compressing backup...${NC}"

    COMPRESSED_FILE="${BACKUP_BASE_PATH}/${BACKUP_NAME}.zip"

    cd "${BACKUP_BASE_PATH}" || exit 1

    if zip -r -q "${COMPRESSED_FILE}" "$(basename "${TEMP_BACKUP_DIR}")" 2>&1 | tee -a "${LOG_FILE}"; then
        COMPRESSED_SIZE=$(du -h "${COMPRESSED_FILE}" | cut -f1)
        log "${GREEN}✓ Backup compressed successfully (${COMPRESSED_SIZE})${NC}"

        # Remove temporary directory
        rm -rf "${TEMP_BACKUP_DIR}"

        echo "${COMPRESSED_FILE}"
        return 0
    else
        log "${RED}✗ Compression failed${NC}"
        return 1
    fi
}

# Move uncompressed backup
move_backup() {
    FINAL_DIR="${BACKUP_BASE_PATH}/${BACKUP_NAME}"

    if mv "${TEMP_BACKUP_DIR}" "${FINAL_DIR}" 2>&1 | tee -a "${LOG_FILE}"; then
        BACKUP_SIZE=$(du -sh "${FINAL_DIR}" | cut -f1)
        log "${GREEN}✓ Backup moved to: ${FINAL_DIR} (${BACKUP_SIZE})${NC}"
        echo "${FINAL_DIR}"
        return 0
    else
        log "${RED}✗ Failed to move backup${NC}"
        return 1
    fi
}

# Cleanup old backups
cleanup_old_backups() {
    log "${YELLOW}Cleaning up old backups...${NC}"

    # Find backups older than retention days
    CUTOFF_DATE=$(date -d "${RETENTION_DAYS} days ago" +%Y%m%d)

    DELETED_COUNT=0

    for backup in "${BACKUP_BASE_PATH}"/kabengosafaris_backup_*; do
        if [ -e "$backup" ]; then
            # Extract date from filename (kabengosafaris_backup_YYYYMMDD_HHMMSS)
            BACKUP_DATE=$(basename "$backup" | grep -oP '\d{8}' | head -1)

            if [ -n "$BACKUP_DATE" ] && [ "$BACKUP_DATE" -lt "$CUTOFF_DATE" ]; then
                if rm -rf "$backup" 2>&1 | tee -a "${LOG_FILE}"; then
                    log "  ${GREEN}✓${NC} Deleted old backup: $(basename "$backup")"
                    ((DELETED_COUNT++))
                fi
            fi
        fi
    done

    # Also check max count retention
    BACKUP_COUNT=$(find "${BACKUP_BASE_PATH}" -maxdepth 1 -name "kabengosafaris_backup_*" | wc -l)

    if [ "$BACKUP_COUNT" -gt "$RETENTION_MAX_COUNT" ]; then
        EXCESS_COUNT=$((BACKUP_COUNT - RETENTION_MAX_COUNT))

        # Delete oldest backups
        find "${BACKUP_BASE_PATH}" -maxdepth 1 -name "kabengosafaris_backup_*" -printf '%T+ %p\n' | \
            sort | head -n "$EXCESS_COUNT" | cut -d' ' -f2- | \
            while read -r old_backup; do
                if rm -rf "$old_backup" 2>&1 | tee -a "${LOG_FILE}"; then
                    log "  ${GREEN}✓${NC} Deleted excess backup: $(basename "$old_backup")"
                    ((DELETED_COUNT++))
                fi
            done
    fi

    if [ $DELETED_COUNT -gt 0 ]; then
        log "${GREEN}✓ Cleanup completed: ${DELETED_COUNT} old backups deleted${NC}"
    else
        log "${GREEN}✓ No old backups to clean up${NC}"
    fi
}

# Send notification email
send_notification() {
    local subject="$1"
    local message="$2"

    if command -v mail &> /dev/null; then
        echo "$message" | mail -s "$subject" "$NOTIFICATION_EMAIL"
        log "Notification sent to: $NOTIFICATION_EMAIL"
    else
        log "${YELLOW}Warning: 'mail' command not found. Cannot send notification.${NC}"
        log "Install mailutils: sudo apt-get install mailutils"
    fi
}

# Main execution
main() {
    print_banner

    DB_SUCCESS=true
    FILES_SUCCESS=true

    # Perform backups
    if [ "$BACKUP_DB" = true ]; then
        if ! backup_database; then
            DB_SUCCESS=false
        fi
    fi

    if [ "$BACKUP_FILES" = true ]; then
        if ! backup_files; then
            FILES_SUCCESS=false
        fi
    fi

    # Check if any backup succeeded
    if [ "$DB_SUCCESS" = false ] && [ "$FILES_SUCCESS" = false ]; then
        log ""
        log "${RED}╔════════════════════════════════════════════════════════════════════╗${NC}"
        log "${RED}║                                                                    ║${NC}"
        log "${RED}║                    BACKUP PROCESS FAILED                           ║${NC}"
        log "${RED}║                                                                    ║${NC}"
        log "${RED}╚════════════════════════════════════════════════════════════════════╝${NC}"

        # Send failure notification
        if [ "$NOTIFY_ON_FAILURE" = true ]; then
            send_notification "Backup Failed - Kabenga Safaris" \
                "Backup failed at $(date). Check logs: ${LOG_FILE}"
        fi

        # Cleanup failed backup
        rm -rf "${TEMP_BACKUP_DIR}"

        exit 1
    fi

    # Compress or move backup
    if [ "$COMPRESS" = true ]; then
        FINAL_BACKUP_PATH=$(compress_backup)
    else
        FINAL_BACKUP_PATH=$(move_backup)
    fi

    # Cleanup old backups
    if [ "$CLEANUP" = true ]; then
        cleanup_old_backups
    fi

    # Print success banner
    log ""
    log "${GREEN}╔════════════════════════════════════════════════════════════════════╗${NC}"
    log "${GREEN}║                                                                    ║${NC}"
    log "${GREEN}║            BACKUP PROCESS COMPLETED SUCCESSFULLY                   ║${NC}"
    log "${GREEN}║                                                                    ║${NC}"
    log "${GREEN}╚════════════════════════════════════════════════════════════════════╝${NC}"
    log ""
    log "Backup completed at: $(date)"
    log "Backup location: ${FINAL_BACKUP_PATH}"
    log "Log file: ${LOG_FILE}"

    # Send success notification
    if [ "$NOTIFY_ON_SUCCESS" = true ]; then
        send_notification "Backup Successful - Kabenga Safaris" \
            "Backup completed successfully at $(date). Location: ${FINAL_BACKUP_PATH}"
    fi
}

# Run main function
main
