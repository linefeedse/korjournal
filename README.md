# Kilometerkoll

This repository contains:
* The Python (Django) code for the backend on kilometerkoll.se
* The Java code for the Kilometerkoll android app.

Feel free to poke around and see how it works. There are no UX/UI support files, however, that means no:
- Icons and images
- Javascript
- CSS
- Android resource files (xml)

## Points Of Interest

### The OCR of Odometer images
See how the code desperately tries to crop the photos you snap, to get Tesseract to look at only the odometer and not the garbage around it: [OCRengine.java](./androidapp/app/src/main/java/se/linefeed/korjournal/OCREngine.java)
### The Tesla API code
See here how the app handles your credentials: [TeslaAPI.java](androidapp/app/src/main/java/se/linefeed/korjournal/api/TeslaAPI.java) and [SettingsActivity.java](androidapp/app/src/main/java/se/linefeed/korjournal/SettingsActivity.java) (It deletes the password after creating an Access token)