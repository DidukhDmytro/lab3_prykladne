CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS cars (
    vin VARCHAR(64) PRIMARY KEY,
    make VARCHAR(100),
    model VARCHAR(100),
    year INT NOT NULL,
    owner_id UUID NOT NULL REFERENCES customers(id)
);

CREATE TABLE IF NOT EXISTS mechanics (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    specialization VARCHAR(32) NOT NULL
        CHECK (specialization IN ('ENGINE', 'BRAKES', 'ELECTRICS', 'SUSPENSION', 'GENERAL')),
    busy BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS repair_orders (
    id UUID PRIMARY KEY,
    car_vin VARCHAR(64) NOT NULL REFERENCES cars(vin),
    mechanic_id UUID REFERENCES mechanics(id),
    status VARCHAR(32) NOT NULL
        CHECK (status IN ('CREATED', 'DIAGNOSED', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    priority VARCHAR(32) NOT NULL
        CHECK (priority IN ('NORMAL', 'URGENT')),
    final_price NUMERIC(12, 2),
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS service_works (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES repair_orders(id) ON DELETE CASCADE,
    position INT NOT NULL,
    description VARCHAR(255) NOT NULL,
    parts_cost NUMERIC(12, 2) NOT NULL CHECK (parts_cost >= 0),
    labor_cost NUMERIC(12, 2) NOT NULL CHECK (labor_cost >= 0),
    required_specialization VARCHAR(32) NOT NULL
        CHECK (required_specialization IN ('ENGINE', 'BRAKES', 'ELECTRICS', 'SUSPENSION', 'GENERAL')),
    status VARCHAR(32) NOT NULL
        CHECK (status IN ('PENDING', 'COMPLETED'))
);

CREATE INDEX IF NOT EXISTS idx_repair_orders_status ON repair_orders(status);
CREATE INDEX IF NOT EXISTS idx_repair_orders_car_vin ON repair_orders(car_vin);
CREATE INDEX IF NOT EXISTS idx_service_works_order_id ON service_works(order_id);
