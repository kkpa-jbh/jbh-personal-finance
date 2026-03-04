-- Add bilingual description column to categories table
ALTER TABLE finance.categories
    ADD COLUMN IF NOT EXISTS description JSONB NULL;

-- Populate descriptions for existing system categories (seeded in 002)
UPDATE finance.categories
SET description = '{"en": "Any income that does not fit another category", "es": "Cualquier ingreso que no encaja en otra categoría"}'
WHERE alias = 'INC_OTHER';
UPDATE finance.categories
SET description = '{"en": "Money received via bank transfer from another account or person", "es": "Dinero recibido por transferencia bancaria desde otra cuenta o persona"}'
WHERE alias = 'INC_TRANSF';
UPDATE finance.categories
SET description = '{"en": "Income generated from investment dividends", "es": "Ingresos generados por dividendos de inversiones"}'
WHERE alias = 'INC_DVDS';
UPDATE finance.categories
SET description = '{"en": "Internal entry used to set the opening balance of an account; not a real transaction", "es": "Entrada interna para establecer el saldo inicial de una cuenta; no es una transacción real"}'
WHERE alias = 'INC_INIT_BALANCE';
UPDATE finance.categories
SET description = '{"en": "Money sent via bank transfer to another account", "es": "Dinero enviado por transferencia bancaria a otra cuenta"}'
WHERE alias = 'EXP_TRANSF';
UPDATE finance.categories
SET description = '{"en": "Tax withheld at source by a financial institution (retefuente)", "es": "Impuesto retenido en la fuente por una entidad financiera (retefuente)"}'
WHERE alias = 'EXP_RTFTE';
UPDATE finance.categories
SET description = '{"en": "Full withdrawal of an investment to close the product entirely", "es": "Retiro total de una inversión para cerrar el producto completamente"}'
WHERE alias = 'INV_TO_CLOSE_IT';
UPDATE finance.categories
SET description = '{"en": "Transaction not yet classified; review and reassign to the correct category", "es": "Transacción aún no clasificada; revisar y reasignar a la categoría correcta"}'
WHERE alias = 'UNK';

-- Add income categories
INSERT INTO finance.categories (source, alias, active, display_name, description)
VALUES ('INCOME', 'INC_SALARY', TRUE,
        '{"en": "Salary",                "es": "Salario / Nómina"}',
        '{"en": "Regular employment income: salary, payroll, and wages", "es": "Ingresos laborales regulares: salario, nómina y sueldo"}'),

       ('INCOME', 'INC_RENTAL', TRUE,
        '{"en": "Rental Income",         "es": "Ingreso por Arriendo"}',
        '{"en": "Income received from renting out a property", "es": "Ingresos recibidos por el arrendamiento de un inmueble"}'),

       ('INCOME', 'INC_FREELANCE', TRUE,
        '{"en": "Freelance",             "es": "Freelance / Independiente"}',
        '{"en": "Income from freelance work, consulting, or independent services", "es": "Ingresos por trabajo freelance, consultoría o servicios independientes"}')

ON CONFLICT (source, alias) DO NOTHING;

-- Add consumer expense categories
INSERT INTO finance.categories (source, alias, active, display_name, description)
VALUES ('EXPENSE', 'EXP_GROCERY', TRUE,
        '{"en": "Market",                "es": "Mercado"}',
        '{"en": "Essential food shopping at supermarkets and markets; not dining out", "es": "Compras esenciales de alimentos en supermercados y mercados; no incluye restaurantes"}'),

       ('EXPENSE', 'EXP_RESTAURANTS', TRUE,
        '{"en": "Restaurants",           "es": "Restaurantes"}',
        '{"en": "Dining out at restaurants, cafés, and fast food; not grocery shopping", "es": "Gastos en restaurantes, cafés y comida rápida; no incluye mercado"}'),

       ('EXPENSE', 'EXP_TRANSPORT', TRUE,
        '{"en": "Transport",             "es": "Transporte"}',
        '{"en": "Daily mobility costs: public transit, taxi, Uber, gas, EV charging, tolls, and parking", "es": "Costos de movilidad diaria: transporte público, taxi, Uber, gasolina, carga eléctrica, peajes y parqueadero"}'),

       ('EXPENSE', 'EXP_VEHICLE', TRUE,
        '{"en": "Vehicle Maintenance",   "es": "Mantenimiento Vehicular"}',
        '{"en": "Vehicle ownership costs: repairs, maintenance, and car taxes; car insurance goes to EXP_INSURANCE", "es": "Costos de tenencia vehicular: reparaciones, mantenimiento e impuestos; el seguro va en EXP_INSURANCE"}'),

       ('EXPENSE', 'EXP_RENT', TRUE,
        '{"en": "House Rent",            "es": "Arriendo"}',
        '{"en": "Monthly rent payment for a house or apartment", "es": "Pago mensual de arriendo de casa o apartamento"}'),

       ('EXPENSE', 'EXP_ADMIN', TRUE,
        '{"en": "House Administration",  "es": "Administración"}',
        '{"en": "Monthly building or condo administration fees", "es": "Cuota mensual de administración del edificio o conjunto residencial"}'),

       ('EXPENSE', 'EXP_HEALTH', TRUE,
        '{"en": "Health & Pharmacy",     "es": "Salud y Farmacia"}',
        '{"en": "Medical care, pharmacy purchases, doctor visits, and laboratory tests", "es": "Atención médica, compras en farmacia, consultas médicas y exámenes de laboratorio"}'),

       ('EXPENSE', 'EXP_FITNESS', TRUE,
        '{"en": "Sports & Fitness",      "es": "Deporte y Actividad Física"}',
        '{"en": "Gym memberships, sports equipment, fitness classes, and athletic activities", "es": "Membresías de gimnasio, equipos deportivos, clases de fitness y actividades atléticas"}'),

       ('EXPENSE', 'EXP_CLOTHING', TRUE,
        '{"en": "Clothing & Footwear",   "es": "Ropa y Calzado"}',
        '{"en": "Clothing, shoes, and sportswear purchases", "es": "Compras de ropa, calzado y prendas deportivas"}'),

       ('EXPENSE', 'EXP_UTILITIES', TRUE,
        '{"en": "Public Services",       "es": "Servicios Públicos"}',
        '{"en": "Utility bills: electricity, water, gas, and other public services", "es": "Facturas de servicios públicos: electricidad, agua, gas y otros servicios"}'),

       ('EXPENSE', 'EXP_TELECOM', TRUE,
        '{"en": "Telecommunications",    "es": "Telecomunicaciones"}',
        '{"en": "Mobile plans, internet service, and landline", "es": "Planes de telefonía móvil, servicio de internet y telefonía fija"}'),

       ('EXPENSE', 'EXP_SUBSCRIPTIONS', TRUE,
        '{"en": "Subscriptions",         "es": "Suscripciones"}',
        '{"en": "Recurring digital subscriptions: streaming, software, SaaS, and news", "es": "Suscripciones digitales recurrentes: streaming, software, SaaS y noticias"}'),

       ('EXPENSE', 'EXP_INSURANCE', TRUE,
        '{"en": "Insurance",             "es": "Seguros"}',
        '{"en": "All insurance premiums: life, health, car, home, and travel", "es": "Todas las primas de seguro: vida, salud, automóvil, hogar y viaje"}'),

       ('EXPENSE', 'EXP_ENTERTAINMENT', TRUE,
        '{"en": "Entertainment",         "es": "Entretenimiento"}',
        '{"en": "Leisure and recreation: movies, concerts, events, and hobbies", "es": "Ocio y recreación: cine, conciertos, eventos y pasatiempos"}'),

       ('EXPENSE', 'EXP_ELECTRONICS', TRUE,
        '{"en": "Electronics & Tech",    "es": "Electrónica y Tecnología"}',
        '{"en": "Consumer electronics: phones, computers, gadgets, and accessories", "es": "Electrónica de consumo: teléfonos, computadores, gadgets y accesorios"}'),

       ('EXPENSE', 'EXP_HOME', TRUE,
        '{"en": "Home Expenses",         "es": "Gastos del Hogar"}',
        '{"en": "General home expenses: furniture, appliances, repairs, and unexpected household costs", "es": "Gastos generales del hogar: muebles, electrodomésticos, reparaciones y gastos imprevistos del hogar"}'),

       ('EXPENSE', 'EXP_EDUCATION', TRUE,
        '{"en": "Education",             "es": "Educación"}',
        '{"en": "Books, stationery, school supplies, online courses, and training", "es": "Libros, papelería, útiles escolares, cursos en línea y capacitaciones"}'),

       ('EXPENSE', 'EXP_DELIVERY', TRUE,
        '{"en": "Delivery",              "es": "Domicilios"}',
        '{"en": "Food delivery apps and package courier services", "es": "Aplicaciones de domicilios de comida y servicios de mensajería de paquetes"}'),

       ('EXPENSE', 'EXP_ATM', TRUE,
        '{"en": "ATM Withdrawal",        "es": "Retiro ATM"}',
        '{"en": "Cash withdrawn at ATM; actual spending category unknown", "es": "Efectivo retirado en cajero automático; categoría de gasto real desconocida"}'),

       ('EXPENSE', 'EXP_TRAVEL', TRUE,
        '{"en": "Travel",                "es": "Viajes"}',
        '{"en": "Travel expenses: flights, hotels, accommodation, and vacation spending", "es": "Gastos de viaje: vuelos, hoteles, alojamiento y gastos de vacaciones"}'),

       ('EXPENSE', 'EXP_PERSONAL_CARE', TRUE,
        '{"en": "Personal Care",         "es": "Cuidado Personal"}',
        '{"en": "Haircuts, cosmetics, beauty treatments, and personal grooming", "es": "Cortes de cabello, cosméticos, tratamientos de belleza y cuidado personal"}'),

       ('EXPENSE', 'EXP_PETS', TRUE,
        '{"en": "Pets",                  "es": "Mascotas"}',
        '{"en": "Pet care: veterinary visits, food, grooming, and accessories", "es": "Cuidado de mascotas: visitas veterinarias, alimento, peluquería y accesorios"}'),

       ('EXPENSE', 'EXP_GIFTS', TRUE,
        '{"en": "Gifts & Donations",     "es": "Regalos y Donaciones"}',
        '{"en": "Gifts for others, donations, and charitable contributions", "es": "Regalos para otros, donaciones y contribuciones benéficas"}')

ON CONFLICT (source, alias) DO NOTHING;
