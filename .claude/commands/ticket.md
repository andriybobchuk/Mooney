Create a GitHub issue in the **andriybobchuk/Mooney** repository, add it to the **Mobile** project board (#2, repo-level), set the status to **Backlog**, and assign it to **andriybobchuk**.

The user will provide a ticket description as the argument: $ARGUMENTS

## Instructions

1. Parse the user's input to extract:
   - **Title**: A short, clear issue title (derive from the description if not explicit)
   - **Body**: The full description with context. If the user gave a one-liner, expand it slightly with acceptance criteria or context based on your understanding of the Mooney codebase.

2. Create the issue:
   ```
   gh issue create --repo andriybobchuk/Mooney --title "<title>" --body "<body>" --assignee andriybobchuk
   ```

3. Add the issue to the Mobile project and set Status to "Backlog":
   ```
   # Get the item ID after adding to project
   gh project item-add 2 --owner andriybobchuk --url <issue_url> --format json

   # Set Status field to "Ready"
   gh project item-edit --project-id PVT_kwHOBDPonc4BTkHl --id <item_id> --field-id PVTSSF_lAHOBDPonc4BTkHlzhAyQx8 --single-select-option-id f75ad846
   ```

4. Report back with:
   - Issue number and URL
   - Confirmation it's on the board in Backlog

## Field Reference (Mobile Project #2, repo-level)
- **Project ID**: `PVT_kwHOBDPonc4BTkHl`
- **Status field ID**: `PVTSSF_lAHOBDPonc4BTkHlzhAyQx8`
  - Backlog: `f75ad846`, Ready: `61e4505c`, In progress: `47fc9ee4`, In review: `df73e18b`, Done: `98236657`
- **Priority field ID**: `PVTSSF_lAHOBDPonc4BTkHlzhAyQ6g` (P0: `79628723`, P1: `0a877460`, P2: `da944a9c`)
- **Size field ID**: `PVTSSF_lAHOBDPonc4BTkHlzhAyQ6k` (XS: `6c6483d2`, S: `f784b110`, M: `7515a9f1`, L: `817d0097`, XL: `db339eb2`)

## Optional fields
If the user mentions priority (P0/P1/P2) or size (XS/S/M/L/XL), set those fields too using the IDs above. If not mentioned, don't set them.

## Example usage
- `/ticket Add CSV import for transactions` → creates issue titled "Add CSV import for transactions", assigns to andriybobchuk, adds to Current Sprint
- `/ticket P1 S: Fix dark mode contrast on analytics screen` → same + sets Priority to P1 and Size to S
