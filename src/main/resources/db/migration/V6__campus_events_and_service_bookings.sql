CREATE TABLE campus_events (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    title VARCHAR(180) NOT NULL,
    summary VARCHAR(600) NOT NULL,
    category VARCHAR(30) NOT NULL,
    venue VARCHAR(180) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    capacity INTEGER NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_event_category CHECK (category IN ('ACADEMIC', 'CAREERS', 'WELLBEING', 'SOCIAL')),
    CONSTRAINT ck_event_capacity CHECK (capacity > 0),
    CONSTRAINT ck_event_time CHECK (ends_at > starts_at),
    CONSTRAINT uq_event_occurrence UNIQUE (university_id, title, starts_at)
);

CREATE INDEX idx_campus_events_university_time
    ON campus_events(university_id, published, starts_at);

CREATE TABLE event_registrations (
    event_id UUID NOT NULL REFERENCES campus_events(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES user_accounts(id),
    status VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (event_id, user_id),
    CONSTRAINT ck_event_registration_status CHECK (status IN ('REGISTERED', 'CANCELLED'))
);

CREATE INDEX idx_event_registrations_user
    ON event_registrations(user_id, status, registered_at DESC);

CREATE TABLE service_offerings (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    service_type VARCHAR(30) NOT NULL,
    title VARCHAR(180) NOT NULL,
    provider_name VARCHAR(180) NOT NULL,
    description VARCHAR(600) NOT NULL,
    location VARCHAR(180) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_service_type CHECK (service_type IN ('APPOINTMENT', 'FACILITY', 'CAMPUS')),
    CONSTRAINT uq_service_title UNIQUE (university_id, title)
);

CREATE INDEX idx_service_offerings_university
    ON service_offerings(university_id, active, service_type);

CREATE TABLE service_slots (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES service_offerings(id) ON DELETE CASCADE,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    capacity INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_service_slot_capacity CHECK (capacity > 0),
    CONSTRAINT ck_service_slot_time CHECK (ends_at > starts_at),
    CONSTRAINT uq_service_slot UNIQUE (service_id, starts_at)
);

CREATE INDEX idx_service_slots_service_time
    ON service_slots(service_id, active, starts_at);

CREATE TABLE service_bookings (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL REFERENCES universities(id),
    slot_id UUID NOT NULL REFERENCES service_slots(id),
    user_id UUID NOT NULL REFERENCES user_accounts(id),
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    booked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_service_booking UNIQUE (slot_id, user_id),
    CONSTRAINT ck_service_booking_status CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

CREATE INDEX idx_service_bookings_user
    ON service_bookings(user_id, status, booked_at DESC);

ALTER TABLE notifications DROP CONSTRAINT ck_notification_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notification_type CHECK (
    notification_type IN (
        'CONNECTION_REQUEST', 'CONNECTION_ACCEPTED', 'MESSAGE', 'SAFETY',
        'EVENT', 'BOOKING'
    )
);
