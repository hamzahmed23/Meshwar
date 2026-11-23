const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const PASSENGER_UID = "passenger_101";
const DRIVER_UID = "driver_202";
const OTHER_DRIVER_UID = "driver_303";
const ADMIN_EMAIL = "mahm0ud6tr6g@gmail.com";
const ADMIN_UID = "admin_999";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any D-RIDE collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("rides").where("status", "==", "searching").get());
  await assertFails(
    unauthDb.collection("users").doc(PASSENGER_UID).set({
      userId: PASSENGER_UID,
      fullName: "Hacker",
      activeMode: "passenger",
      roles: ["passenger"],
      ratingAvg: 5.0,
      ratingCount: 0,
      completedRidesCount: 0,
      suspended: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile isolation and privilege escalation guard", async () => {
  const passengerDb = testEnv.authenticatedContext(PASSENGER_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    passengerDb.collection("users").doc(PASSENGER_UID).set({
      userId: PASSENGER_UID,
      fullName: "أحمد محمود",
      phone: "01012345678",
      activeMode: "passenger",
      roles: ["passenger"],
      ratingAvg: 5.0,
      ratingCount: 0,
      completedRidesCount: 0,
      suspended: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Ghost field & self-assigned admin role must fail
  await assertFails(
    passengerDb.collection("users").doc(PASSENGER_UID).update({
      roles: ["passenger", "admin"],
      activeMode: "admin",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Another user cannot read passenger PII
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  await assertFails(driverDb.collection("users").doc(PASSENGER_UID).get());
});

test("Unapproved drivers cannot go online or submit ride offers", async () => {
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  const passengerDb = testEnv.authenticatedContext(PASSENGER_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Driver registers with pending_review
  await assertSucceeds(
    driverDb.collection("driver_profiles").doc(DRIVER_UID).set({
      driverId: DRIVER_UID,
      fullName: "كريم حسن",
      phone: "01098765432",
      nationalId: "29501012500123",
      vehicleCategory: "tuk_tuk",
      vehicleModel: "باجاج RE",
      vehicleColor: "أزرق",
      vehiclePlate: "أ س ط 452",
      approvalStatus: "pending_review",
      online: false,
      completedRidesCount: 0,
      totalGrossEgp: 0,
      totalCommissionEgp: 0,
      totalNetEgp: 0,
      ratingAvg: 5.0,
      ratingCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Driver cannot self-approve
  await assertFails(
    driverDb.collection("driver_profiles").doc(DRIVER_UID).update({
      approvalStatus: "approved",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Unapproved driver cannot go online in driver_presence
  await assertFails(
    driverDb.collection("driver_presence").doc(DRIVER_UID).set({
      driverId: DRIVER_UID,
      cityId: "dayrout",
      vehicleCategory: "tuk_tuk",
      online: true,
      latitude: 27.5561,
      longitude: 30.8075,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Create searching ride as passenger
  await assertSucceeds(
    passengerDb.collection("rides").doc("ride_1").set({
      passengerId: PASSENGER_UID,
      passengerName: "أحمد محمود",
      cityId: "dayrout",
      pickupAddress: "محطة قطار ديروط",
      pickupLat: 27.5561,
      pickupLng: 30.8075,
      destinationAddress: "مستشفى ديروط المركزي",
      destinationLat: 27.559,
      destinationLng: 30.812,
      distanceKm: 2.1,
      vehicleCategory: "tuk_tuk",
      paymentMethod: "cash",
      proposedFareEgp: 25,
      status: "searching",
      participantUids: [PASSENGER_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Unapproved driver cannot create offer
  await assertFails(
    driverDb.collection("rides").doc("ride_1").collection("offers").doc("offer_1").set({
      rideId: "ride_1",
      passengerId: PASSENGER_UID,
      driverId: DRIVER_UID,
      driverName: "كريم حسن",
      driverRating: 5.0,
      vehicleCategory: "tuk_tuk",
      vehicleSummary: "باجاج RE أزرق",
      vehiclePlate: "أ س ط 452",
      offeredFareEgp: 25,
      counterOffer: false,
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Email spoofing with unverified admin email is rejected; verified admin approves driver", async () => {
  const spoofedAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    driverDb.collection("driver_profiles").doc(DRIVER_UID).set({
      driverId: DRIVER_UID,
      fullName: "كريم حسن",
      phone: "01098765432",
      nationalId: "29501012500123",
      vehicleCategory: "tuk_tuk",
      vehicleModel: "باجاج RE",
      vehicleColor: "أزرق",
      vehiclePlate: "أ س ط 452",
      approvalStatus: "pending_review",
      online: false,
      completedRidesCount: 0,
      totalGrossEgp: 0,
      totalCommissionEgp: 0,
      totalNetEgp: 0,
      ratingAvg: 5.0,
      ratingCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    spoofedAdminDb.collection("driver_profiles").doc(DRIVER_UID).update({
      approvalStatus: "approved",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    realAdminDb.collection("driver_profiles").doc(DRIVER_UID).update({
      approvalStatus: "approved",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Approved driver submits offer, passenger atomically accepts, driver completes ride and records commission", async () => {
  const passengerDb = testEnv.authenticatedContext(PASSENGER_UID).firestore();
  const driverDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  const otherDriverDb = testEnv.authenticatedContext(OTHER_DRIVER_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 10000);

  // Admin sets 10% commission rule
  await assertSucceeds(
    adminDb.collection("config").doc("commission_rules").set({
      commissionRatePercent: 10,
      updatedBy: ADMIN_UID,
      updatedAt: now,
    })
  );

  // Seed approved driver profile
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("driver_profiles").doc(DRIVER_UID).set({
      driverId: DRIVER_UID,
      fullName: "كريم حسن",
      phone: "01098765432",
      nationalId: "29501012500123",
      vehicleCategory: "tuk_tuk",
      vehicleModel: "باجاج RE",
      vehicleColor: "أزرق",
      vehiclePlate: "أ س ط 452",
      approvalStatus: "approved",
      online: true,
      completedRidesCount: 0,
      totalGrossEgp: 0,
      totalCommissionEgp: 0,
      totalNetEgp: 0,
      ratingAvg: 5.0,
      ratingCount: 0,
      createdAt: now,
      updatedAt: now,
    });
  });

  // Passenger creates searching ride
  await assertSucceeds(
    passengerDb.collection("rides").doc("ride_99").set({
      passengerId: PASSENGER_UID,
      passengerName: "أحمد محمود",
      cityId: "dayrout",
      pickupAddress: "محطة قطار ديروط",
      pickupLat: 27.5561,
      pickupLng: 30.8075,
      destinationAddress: "مستشفى ديروط المركزي",
      destinationLat: 27.559,
      destinationLng: 30.812,
      distanceKm: 2.1,
      vehicleCategory: "tuk_tuk",
      paymentMethod: "cash",
      proposedFareEgp: 25,
      status: "searching",
      participantUids: [PASSENGER_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Approved driver queries searching rides in Dayrout
  await assertSucceeds(
    driverDb.collection("rides").where("status", "==", "searching").where("cityId", "==", "dayrout").get()
  );

  // Driver submits counteroffer (30 EGP)
  await assertSucceeds(
    driverDb.collection("rides").doc("ride_99").collection("offers").doc("offer_1").set({
      rideId: "ride_99",
      passengerId: PASSENGER_UID,
      driverId: DRIVER_UID,
      driverName: "كريم حسن",
      driverRating: 5.0,
      vehicleCategory: "tuk_tuk",
      vehicleSummary: "باجاج RE أزرق",
      vehiclePlate: "أ س ط 452",
      offeredFareEgp: 30,
      counterOffer: true,
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Passenger accepts offer and assigns driver
  await assertSucceeds(
    passengerDb.collection("rides").doc("ride_99").update({
      status: "offer_selected",
      assignedDriverId: DRIVER_UID,
      assignedDriverName: "كريم حسن",
      assignedDriverVehicle: "باجاج RE أزرق",
      assignedDriverPlate: "أ س ط 452",
      assignedDriverRating: 5.0,
      agreedFareEgp: 30,
      participantUids: [PASSENGER_UID, DRIVER_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Second driver cannot steal an already assigned ride
  await assertFails(
    otherDriverDb.collection("rides").doc("ride_99").update({
      status: "offer_selected",
      assignedDriverId: OTHER_DRIVER_UID,
      agreedFareEgp: 20,
      participantUids: [PASSENGER_UID, OTHER_DRIVER_UID],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Driver cannot jump straight from offer_selected to completed (controlled lifecycle!)
  await assertFails(
    driverDb.collection("rides").doc("ride_99").update({
      status: "completed",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Valid lifecycle progression
  await assertSucceeds(
    driverDb.collection("rides").doc("ride_99").update({
      status: "driver_arriving",
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertSucceeds(
    driverDb.collection("rides").doc("ride_99").update({
      status: "driver_arrived",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertSucceeds(
    driverDb.collection("rides").doc("ride_99").update({
      status: "in_progress",
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertSucceeds(
    driverDb.collection("rides").doc("ride_99").update({
      status: "completed",
      commissionRatePercent: 10,
      commissionAmountEgp: 3,
      driverNetEgp: 27,
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Financial transaction record
  await assertSucceeds(
    driverDb.collection("financial_transactions").doc("tx_ride_99").set({
      rideId: "ride_99",
      driverId: DRIVER_UID,
      passengerId: PASSENGER_UID,
      cityId: "dayrout",
      grossFareEgp: 30,
      commissionRatePercent: 10,
      commissionAmountEgp: 3,
      driverNetEgp: 27,
      paymentMethod: "cash",
      createdAt: new Date(Date.now() - 100),
    })
  );
});

test("User saved favorite places enforce strict owner isolation and CRUD validation", async () => {
  const passengerDb = testEnv.authenticatedContext(PASSENGER_UID).firestore();
  const otherDb = testEnv.authenticatedContext(DRIVER_UID).firestore();
  const now = new Date(Date.now() - 5000);

  const homeRef = passengerDb.collection("users").doc(PASSENGER_UID).collection("saved_places").doc("home");
  await assertSucceeds(
    homeRef.set({
      userId: PASSENGER_UID,
      placeKey: "home",
      label: "المنزل",
      address: "شارع بورسعيد - ديروط",
      latitude: 27.5561,
      longitude: 30.8075,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner updates saved place preserving createdAt
  await assertSucceeds(
    homeRef.update({
      address: "ميدان المحطة - ديروط",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Other user cannot read, overwrite, or delete passenger's saved place
  const otherTargetRef = otherDb.collection("users").doc(PASSENGER_UID).collection("saved_places").doc("home");
  await assertFails(otherTargetRef.get());
  await assertFails(otherTargetRef.delete());

  // Owner can delete saved place
  await assertSucceeds(homeRef.delete());
});
