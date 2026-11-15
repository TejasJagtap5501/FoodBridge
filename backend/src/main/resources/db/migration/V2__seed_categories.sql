-- =====================================================================
-- FoodBridge V2 - seed reference data (food categories).
-- Demo accounts are created by the application's DemoDataSeeder so that
-- BCrypt hashes and timestamps stay consistent and can be disabled with
-- SEED_DEMO_DATA=false.
-- =====================================================================

INSERT INTO food_categories (name, slug, description, active, display_order, created_at, updated_at) VALUES
    ('Cooked Meals',        'cooked-meals',        'Ready-to-eat prepared dishes such as rice, curries, biryani and pasta.', TRUE, 10, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Bakery & Bread',      'bakery-bread',        'Breads, cakes, pastries and other baked goods.',                        TRUE, 20, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Fresh Produce',       'fresh-produce',       'Fruit and vegetables that are still fit for consumption.',              TRUE, 30, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Packaged & Dry Goods','packaged-dry-goods',  'Sealed packaged food, grains, pulses, biscuits and shelf-stable items.', TRUE, 40, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Dairy & Eggs',        'dairy-eggs',          'Milk, cheese, yogurt, butter and eggs kept under cold chain.',          TRUE, 50, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Beverages',           'beverages',           'Bottled, canned or freshly prepared drinks.',                           TRUE, 60, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
