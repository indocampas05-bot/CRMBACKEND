# IndoCRM Backend - Excel Import Fix

This backend patch fixes Super Admin Excel lead import so the uploaded Excel row is preserved in `Lead.excelData` and returned by the Lead API.

## Main changes

1. Excel parser preserves original column names instead of converting every column name to a normalized key.
2. Multi-row/merged headers are preserved; duplicate columns receive a parent prefix or a unique suffix so no value is overwritten.
3. `LeadService.importExcel()` stores the complete row in `excelData`.
4. Existing leads are matched by normalized mobile number and updated directly.
5. New leads are created and then receive the complete `excelData` snapshot.
6. District-based telecaller assignment remains enabled.
7. Known Excel fields such as Organization/School, Address, Phone, District, UP1-UP5, etc. are also synchronized into the normal Lead fields for existing frontend screens.
8. Unknown Excel columns are not discarded.

## Important frontend requirement

The Super Admin upload must send the ORIGINAL `.xlsx`/`.xls` file to:

`POST /api/leads/import-excel`

using multipart field name `file`.

Do not loop through rows and call the normal `POST /api/leads` endpoint for Excel import.
