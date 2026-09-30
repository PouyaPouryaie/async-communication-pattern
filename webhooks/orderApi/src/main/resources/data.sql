INSERT INTO products (id, name, price, stock_quantity) VALUES
    (1, 'Wireless Mouse', 24.99, 50),
    (2, 'Mechanical Keyboard', 89.99, 30),
    (3, '27-inch Monitor', 249.99, 15)
ON CONFLICT (id) DO NOTHING;
