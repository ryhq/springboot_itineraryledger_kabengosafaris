# Permission Configuration Files

This directory contains JSON configuration files that define the permissions initialized at application startup.

## Files

### entities.json
Contains the list of entities that require standard CRUD permissions (CREATE, READ, UPDATE, DELETE).

**Format:**
```json
{
  "entities": [
    "USER",
    "ROLE",
    "PERMISSION",
    ...
  ]
}
```

When you add a new entity to this list, the PermissionInitializer will automatically create 4 permissions:
- `CREATE_{ENTITY}`
- `READ_{ENTITY}`
- `UPDATE_{ENTITY}`
- `DELETE_{ENTITY}`

### custom-permissions.json
Contains custom permissions that don't follow the standard CRUD pattern. These are task-based or workflow-specific permissions.

**Format:**
```json
{
  "customPermissions": [
    {
      "name": "PERMISSION_NAME",
      "action": "ACTION_TYPE",
      "entity": "ENTITY_NAME",
      "description": "Human-readable description"
    }
  ]
}
```

**Available Actions:**
- `CREATE` - Creating resources
- `READ` - Viewing/reading resources
- `UPDATE` - Editing/updating resources
- `DELETE` - Deleting resources
- `EXECUTE` - Executing operations
- `SUBMIT` - Submitting for approval
- `AMEND` - Amending submitted items
- `CANCEL` - Canceling/terminating
- `EXPORT` - Exporting data
- `PRINT` - Printing documents

## Adding New Entities

To add a new entity that needs CRUD permissions:

1. Open `entities.json`
2. Add the entity name (in UPPERCASE_WITH_UNDERSCORES format) to the `entities` array
3. Save the file
4. Restart the application - permissions will be created automatically

Example:
```json
{
  "entities": [
    "USER",
    "ROLE",
    "NEW_ENTITY_NAME"  // Add here
  ]
}
```

This will create:
- `CREATE_NEW_ENTITY_NAME`
- `READ_NEW_ENTITY_NAME`
- `UPDATE_NEW_ENTITY_NAME`
- `DELETE_NEW_ENTITY_NAME`

## Adding Custom Permissions

To add a custom permission that doesn't fit the CRUD pattern:

1. Open `custom-permissions.json`
2. Add a new permission object to the `customPermissions` array
3. Save the file
4. Restart the application

Example:
```json
{
  "customPermissions": [
    {
      "name": "APPROVE_BOOKING",
      "action": "UPDATE",
      "entity": "BOOKING",
      "description": "Allows approving a booking after review"
    }
  ]
}
```

## Best Practices

1. **Entity Names:** Use UPPERCASE with underscores (e.g., `EMAIL_ACCOUNT`, not `emailAccount`)
2. **Permission Names:** Follow the pattern `{ACTION}_{ENTITY}` or descriptive names for custom permissions
3. **Descriptions:** Write clear, user-friendly descriptions explaining what the permission allows
4. **Action Selection:** Choose the most appropriate action type that represents the permission's purpose

## Validation

The application will:
- Load these JSON files at startup
- Log the number of entities and custom permissions loaded
- Create missing permissions (idempotent - won't duplicate existing permissions)
- Log errors if JSON files are malformed or missing

Check the application logs for:
- `Loaded X entities from entities.json`
- `Loaded X custom permissions from custom-permissions.json`
- `Permission initialization complete: X permissions created, Y already existed`
