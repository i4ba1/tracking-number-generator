// Create indexes for optimal performance
db = db.getSiblingDB('tracking_numbers');

// Create collection with validation
db.createCollection("tracking_numbers", {
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["trackingNumber", "originCountryId", "destinationCountryId", "weight"],
            properties: {
                trackingNumber: {
                    bsonType: "string",
                    pattern: "^[A-Z0-9]{1,16}$",
                    description: "Tracking number must match the required pattern"
                },
                originCountryId: {
                    bsonType: "string",
                    pattern: "^[A-Z]{2}$",
                    description: "Origin country must be ISO 3166-1 alpha-2 format"
                },
                destinationCountryId: {
                    bsonType: "string",
                    pattern: "^[A-Z]{2}$",
                    description: "Destination country must be ISO 3166-1 alpha-2 format"
                },
                weight: {
                    bsonType: "double",
                    minimum: 0.001,
                    maximum: 999.999,
                    description: "Weight must be between 0.001 and 999.999 kg"
                }
            }
        }
    }
});

// Create unique index on tracking number for fast lookups and uniqueness
db.tracking_numbers.createIndex(
    { "trackingNumber": 1 },
    { unique: true, name: "idx_tracking_number_unique" }
);

// Create compound index for analytics queries
db.tracking_numbers.createIndex(
    { "originCountryId": 1, "destinationCountryId": 1, "createdAt": -1 },
    { name: "idx_route_created" }
);

// Create index on customer for customer-specific queries
db.tracking_numbers.createIndex(
    { "customerId": 1, "createdAt": -1 },
    { name: "idx_customer_created" }
);

// Create index on creation time for time-based queries
db.tracking_numbers.createIndex(
    { "createdAt": -1 },
    { name: "idx_created_at" }
);

print("MongoDB indexes created successfully");