ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS fk_customers_hobby;

ALTER TABLE customers
    DROP COLUMN IF EXISTS hobby_id;

ALTER TABLE customer_hobbies
    DROP COLUMN IF EXISTS name;
