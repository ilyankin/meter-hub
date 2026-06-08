CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE meters_seq START WITH 1 INCREMENT BY 50;

-- Роли пользователя
CREATE TABLE user_roles
(
    id   BIGINT PRIMARY KEY,
    code VARCHAR(63) NOT NULL UNIQUE,
    CONSTRAINT chk_user_roles_code_not_blank CHECK (TRIM(code) <> '')
);
COMMENT
ON TABLE user_roles IS 'Справочник ролей';
COMMENT
ON COLUMN user_roles.code IS 'Код роли';

-- Дефолтные роли системы
INSERT INTO user_roles (id, code)
VALUES (1, 'ADMIN'),
       (2, 'MANAGER');

-- Пользователи системы
CREATE TABLE users
(
    id         BIGINT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    full_name  VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role_id    BIGINT       NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES user_roles (id),
    CONSTRAINT chk_users_email_not_blank CHECK (TRIM(email) <> ''),
    CONSTRAINT chk_users_full_name_not_blank CHECK (TRIM(full_name) <> ''),
    CONSTRAINT chk_users_password_not_blank CHECK (TRIM(password) <> '')
);

COMMENT
ON TABLE users IS 'Пользователи';
COMMENT
ON COLUMN users.email IS 'Электронная почта';
COMMENT
ON COLUMN users.full_name IS 'ФИО';
COMMENT
ON COLUMN users.password IS 'Пароль';
COMMENT
ON COLUMN users.role_id IS 'Роль (ссылка на справочник roles)';

-- Токены авторизации
CREATE TABLE auth_tokens
(
    id         UUID PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE CHECK (TRIM(token) <> ''),
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT fk_auth_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_auth_tokens_expiry CHECK (expires_at > created_at)
);
COMMENT
ON TABLE auth_tokens IS 'Токены авторизации';
COMMENT
ON COLUMN auth_tokens.user_id IS 'Связь с пользователем';
COMMENT
ON COLUMN auth_tokens.token IS 'Токен доступа';
COMMENT
ON COLUMN auth_tokens.expires_at IS 'Дата истечения токена';

-- Приборы учета электроэнергии
CREATE TABLE meters
(
    id                       BIGINT PRIMARY KEY,
    user_id                  BIGINT       NOT NULL,
    serial_number            VARCHAR(100) NOT NULL UNIQUE CHECK (TRIM(serial_number) <> ''),
    inventory_number         VARCHAR(100),
    manufacture_year         INT,
    transformation_ratio     DECIMAL(10, 2) CHECK (transformation_ratio > 0),
    installation_date        DATE,
    seal_number              VARCHAR(100),
    antimagnetic_seal_number VARCHAR(100),
    installation_location    VARCHAR(500),
    notes                    TEXT,
    gis_id                   VARCHAR(100),
    created_at               TIMESTAMP    NOT NULL,
    updated_at               TIMESTAMP    NOT NULL,
    CONSTRAINT fk_meters_user FOREIGN KEY (user_id) REFERENCES users (id)
);
COMMENT
ON TABLE meters IS 'Приборы учета электроэнергии';
COMMENT
ON COLUMN meters.user_id IS 'Связка с пользователем (ответственный)';
COMMENT
ON COLUMN meters.serial_number IS 'Серийный номер';
COMMENT
ON COLUMN meters.inventory_number IS 'Инвентарный номер';
COMMENT
ON COLUMN meters.manufacture_year IS 'Год изготовления';
COMMENT
ON COLUMN meters.transformation_ratio IS 'Коэффициент трансформации';
COMMENT
ON COLUMN meters.installation_date IS 'Дата установки';
COMMENT
ON COLUMN meters.seal_number IS 'Номер пломбы';
COMMENT
ON COLUMN meters.antimagnetic_seal_number IS 'Номер антимагнитной пломбы';
COMMENT
ON COLUMN meters.installation_location IS 'Место установки';
COMMENT
ON COLUMN meters.notes IS 'Примечание';
COMMENT
ON COLUMN meters.gis_id IS 'ID в ГИС ЖКХ';

-- Справочник тарифных зон
CREATE TABLE tariff_zones
(
    id          BIGINT PRIMARY KEY,
    code        VARCHAR(15) NOT NULL UNIQUE,
    description VARCHAR(255),
    CONSTRAINT chk_tariff_zones_code_not_blank CHECK (TRIM(code) <> '')
);
COMMENT
ON TABLE tariff_zones IS 'Справочник тарифных зон';
COMMENT
ON COLUMN tariff_zones.code IS 'Код зоны (T1, T2, T3, ...)';
COMMENT
ON COLUMN tariff_zones.description IS 'Описание зоны';

-- Базовые тарифные зоны
INSERT INTO tariff_zones (id, code, description)
VALUES (1, 'T1', 'День'),
       (2, 'T2', 'Ночь');

-- Показания приборов учета
CREATE TABLE meter_readings
(
    id           UUID PRIMARY KEY,
    meter_id     BIGINT    NOT NULL,
    reading_date TIMESTAMP NOT NULL,
    created_at   TIMESTAMP NOT NULL,
    CONSTRAINT fk_readings_meter FOREIGN KEY (meter_id) REFERENCES meters (id)
);
COMMENT
ON TABLE meter_readings IS 'Показания приборов учета';
COMMENT
ON COLUMN meter_readings.meter_id IS 'Связка с прибором учета';
COMMENT
ON COLUMN meter_readings.reading_date IS 'Дата снятия показания';

-- Значения показаний по тарифным зонам
CREATE TABLE meter_reading_values
(
    reading_id     UUID           NOT NULL,
    tariff_zone_id BIGINT         NOT NULL,
    reading_value  DECIMAL(12, 3) NOT NULL,
    PRIMARY KEY (reading_id, tariff_zone_id),
    CONSTRAINT fk_reading_values_reading FOREIGN KEY (reading_id) REFERENCES meter_readings (id),
    CONSTRAINT fk_reading_values_zone FOREIGN KEY (tariff_zone_id) REFERENCES tariff_zones (id),
    CONSTRAINT chk_reading_values_non_negative CHECK (reading_value >= 0)
);

COMMENT
ON TABLE meter_reading_values IS 'Значения показаний по тарифным зонам';
COMMENT
ON COLUMN meter_reading_values.reading_id IS 'Связка с показанием';
COMMENT
ON COLUMN meter_reading_values.tariff_zone_id IS 'Связка с тарифной зоной';
COMMENT
ON COLUMN meter_reading_values.reading_value IS 'Значение показания по зоне';

CREATE INDEX idx_users_role ON users (role_id);
CREATE INDEX idx_auth_tokens_token ON auth_tokens (token);
CREATE INDEX idx_auth_tokens_user ON auth_tokens (user_id);
CREATE INDEX idx_meters_user ON meters (user_id);
CREATE INDEX idx_meter_readings_meter ON meter_readings (meter_id);
CREATE INDEX idx_meter_readings_date ON meter_readings (reading_date);
CREATE INDEX idx_reading_values_zone ON meter_reading_values (tariff_zone_id);