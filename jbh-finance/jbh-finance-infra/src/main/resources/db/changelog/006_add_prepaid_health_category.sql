-- Add prepaid medicine / private health plan expense category
INSERT INTO finance.categories (source, alias, active, display_name, description)
VALUES ('EXPENSE', 'EXP_PREPAID_HEALTH', TRUE,
        '{"en": "Prepaid Health Plan",          "es": "Medicina Prepagada / Póliza de Salud"}',
        '{"en": "Monthly prepaid medicine or private health plan subscription (e.g., Colsanitas, Compensar)", "es": "Suscripción mensual a medicina prepagada o póliza de salud privada (ej. Colsanitas, Compensar)"}')

ON CONFLICT (source, alias) DO NOTHING;
