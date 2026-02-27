-- Replace category_type text column with category_id FK reference to finance.categories
ALTER TABLE finance.movements DROP COLUMN category_type;

ALTER TABLE finance.movements
    ADD COLUMN category_id INTEGER NULL;

ALTER TABLE finance.movements
    ADD CONSTRAINT fk_movements_category_id
        FOREIGN KEY (category_id) REFERENCES finance.categories (id);
