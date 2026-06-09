-- Одно показание на прибор и дату: страховка БД за прикладной проверкой дублей при импорте CSV
ALTER TABLE meter_readings
    ADD CONSTRAINT uq_meter_readings_meter_date UNIQUE (meter_id, reading_date);