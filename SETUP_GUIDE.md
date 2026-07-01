# Smart Hisab Setup & Deployment Guide

This document provides complete instructions for setting up Firebase, configuring Firestore security rules, building the release APK, using the test checklist, and reviewing the folder structure for **Smart Hisab**.

---

## 1. Firebase Setup Steps
Smart Hisab supports full offline-first functionality and seamlessly syncs to the cloud when Firebase is integrated.

1. **Create a Firebase Project:**
   - Go to the [Firebase Console](https://console.firebase.google.com/).
   - Click **Add project** and name it `Smart-Hisab`.
   - Choose whether to enable Google Analytics (optional) and click **Create project**.

2. **Add an Android App:**
   - Click the Android icon in the center of the project overview page to register an app.
   - **Android package name:** Must match the `applicationId` defined in `app/build.gradle.kts`:
     `com.smarthisab.app`
   - **App nickname (Optional):** `Smart Hisab`
   - **Debug signing certificate SHA-1 (Optional but recommended for Google Sign-In/Auth):**
     - You can get this by running `./gradlew signingReport` in your local environment.
   - Click **Register app**.

3. **Download and Place Configuration File:**
   - Download the `google-services.json` file.
   - Place this file in the `app/` directory of your project (e.g., `app/google-services.json`).

4. **Enable Firebase Authentication:**
   - In the Firebase sidebar, go to **Build** -> **Authentication**.
   - Click **Get started**.
   - Enable **Email/Password** as a sign-in provider.

5. **Enable Cloud Firestore Database:**
   - Go to **Build** -> **Firestore Database** and click **Create database**.
   - Choose a location near your target customers.
   - Select **Start in test mode** initially (then replace with the production rules below).

---

## 2. Firestore Security Rules Needed
To ensure data privacy and isolate each merchant's ledgers and sales records, apply these secure rules under the **Rules** tab in your Firebase Firestore console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Isolate all records so that only the authenticated user owning the data can read/write it
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

*These rules guarantee that Merchant A can never read, edit, or delete the inventory, customers, or reports of Merchant B.*

---

## 3. Complete Test Checklist
Use this step-by-step checklist to verify every feature of the Smart Hisab application:

### Authentication Journeys
- [ ] **Signup New User:**
  - Enter shop name, owner name, email, password, select default language (English/Urdu).
  - Check that password requirements are validated.
  - Submit and verify automatic login and routing to the Dashboard.
- [ ] **Logout/Login:**
  - Go to Settings, tap **LOGOUT**. Verify session destruction and routing back to Login.
  - Re-login with the created email/password. Verify instant access.

### Inventory (Product) Management
- [ ] **Add Product:**
  - Tap **Products** in bottom bar, tap **+ Product**.
  - Fill name, purchase price, sale price, starting stock, low stock limit.
  - Verify warning occurs if sale price is less than purchase price, but allows bypass on second tap.
- [ ] **Edit/Delete Product:**
  - Click any product, change parameters, and tap **Save**.
  - Or tap **Delete Product** to verify deletion or archiving.

### Billing & Invoice Generation
- [ ] **Create Unpaid Invoice (100% Udhaar):**
  - Go to **New Bill**, select products, set customer, set paid amount to `0.00`.
  - Tap **Save Bill**. Verify the invoice status is marked as `unpaid` and outstanding balance is correct.
- [ ] **Create Partial Payment Invoice:**
  - Add items to cart. Set paid amount to a fraction of the total.
  - Save invoice and verify status is `partial`.
- [ ] **Check Stock Reduction:**
  - Verify that the stock quantity of purchased products automatically decreased by the sold quantities.
  - Verify that products cannot be sold beyond available stock (preventing negative inventory).

### Customer Udhaar Ledger
- [ ] **Add Customer:**
  - Go to **Customers** tab, tap **+ Customer** icon.
  - Input name, optional phone and address.
- [ ] **Link Invoice to Customer:**
  - Create a bill and type the customer's name to link their profile.
  - Verify the invoice automatically updates the customer's total outstanding udhaar balance.
- [ ] **Add Customer Payment:**
  - Open the customer profile, tap **Payment**, record a cash payment.
  - Verify that the outstanding udhaar balance is correctly decremented in real time.

### Expense Tracker
- [ ] **Add Expense:**
  - Go to Dashboard quick action **Add Expense** or bottom bar.
  - Fill category (Rent, Salary, Bills, Purchase, Other) and amount.
  - Verify today and monthly totals update.

### Reports & Analytics
- [ ] **View Reports Dashboard:**
  - Check Today Sales, Today Profit, Net Profit, and Expenses match the completed invoices.
  - Switch tabs between Today, This Month, and Yearly charts.

### Sharing & PDFs
- [ ] **Generate PDF Invoice:**
  - Click any invoice from history or checkout, choose **A4 PDF** or **Thermal PDF**.
- [ ] **Share PDF / WhatsApp:**
  - Tap Share icon to trigger Android's share chooser for WhatsApp, Email, or Print.

---

## 4. Release APK Build Instructions
Follow these steps to generate a secure production-ready Release APK (`.apk`) or Android App Bundle (`.aab`):

1. **Generate a Keystore (if you don't have one):**
   ```bash
   keytool -genkey -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Configure Environment Variables:**
   Set the following variables in your local building terminal or pipeline:
   - `KEYSTORE_PATH` = Path to your `.jks` file
   - `STORE_PASSWORD` = Your keystore password
   - `KEY_PASSWORD` = Your key alias password
3. **Compile Release Build:**
   ```bash
   gradle assembleRelease
   ```
4. **Locate Build Output:**
   Your release APK will be available in:
   `app/build/outputs/apk/release/app-release.apk`

---

## 5. Project Folder Structure
```
├── app
│   ├── src
│   │   ├── main
│   │   │   ├── java/com/example
│   │   │   │   ├── db/                 # Room DB (ProductDao, CustomerDao, AppDatabase)
│   │   │   │   ├── localization/       # Multilingual translation dictionaries (EN, UR)
│   │   │   │   ├── model/              # Schema & data entities (Product, Customer, Invoice, Expense)
│   │   │   │   ├── repository/         # Repository layers orchestrating Room + Firestore
│   │   │   │   ├── service/            # Firebase service adapters & network connections
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/        # Compose Screen UI components
│   │   │   │   │   └── theme/          # Centralized color tokens, typography, shapes
│   │   │   │   └── viewmodel/          # State engines holding flows
│   │   │   ├── res/                    # Drawables, mipmaps, and string/xml resources
│   │   │   └── AndroidManifest.xml     # Application manifests (permissions, services)
│   ├── build.gradle.kts                # App build configuration, dependencies, signing configs
├── build.gradle.kts                    # Project build configurations
└── settings.gradle.kts                 # Module declarations
```

---

## 6. Known Limitations & Recommendations
- **Offline Batch Sinks:** When offline, invoices and stock quantities are instantly persisted locally. Cloud sync occurs automatically on the next internet-connected write action.
- **Large PDF Renders:** Generating full resolution A4 prints consumes temporary memory; for low-resource active Android devices, the lightweight **Thermal PDF** mode is recommended.
- **Database Migrations:** If modifying Room schemas in the future, make sure to update `databaseVersion` or use migration path rules to avoid local device table resets.
