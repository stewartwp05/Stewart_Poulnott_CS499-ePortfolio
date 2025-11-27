# Enhancement One: Password Security

**Category:** Software Engineering and Design

**Description:** Implemented SHA-256 password hashing with unique salts to secure user authentication in the WeightTracker Android application.

## Files:
- `PasswordHasher.kt` - Utility class for password hashing and validation
- `DatabaseHelper.kt` - Updated database layer with password hashing
- `MainActivity.kt` - Updated registration with password validation
- `narrative.md` - Complete enhancement narrative

## Key Improvements:
- SHA-256 cryptographic hashing
- Unique salt generation per user
- Password validation (8+ chars, uppercase, digit)
- Database migration from version 3 to 4
