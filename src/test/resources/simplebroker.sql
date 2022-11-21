INSERT INTO tax_details(id, flat_capital_gains_tax_rate, tax_residence, taxation_currency, loss_offset_years, loss_offset_cut_off_month, loss_offset_cut_off_day) VALUES (1, 15000000, "HU", "HUF", 2, 1, 1);
-- SIMPLE BROKER
INSERT INTO brokers(id, name) VALUES (555, "simple");

INSERT INTO broker_transfer_fees (id, broker_id, transferred_currency, percent_fee, minimum_fee, maximum_fee, fee_currency) VALUES (555, 555, "EUR", 450000, 2710000000, 66000000000, "HUF");
INSERT INTO broker_transfer_fees (id, broker_id, transferred_currency, percent_fee, minimum_fee, maximum_fee, fee_currency) VALUES (556, 555, "USD", 450000, 2710000000, 66000000000, "HUF");
--INSERT INTO broker_transfer_fees (id, broker_id, transferred_currency, percent_fee, minimum_fee, maximum_fee, fee_currency) VALUES (557, 555, "HUF", 450000, 2710000000, 66000000000, "HUF");

INSERT INTO broker_products(id, broker_id, name, fixed_fee_amt, fixed_fee_currency, fixed_fee_period, balance_fee_percent) VALUES (555, 555, "all accounts", 350000000, "HUF", "MONTHLY", 10000);

INSERT INTO broker_global_fees (id, broker_id, fixed_fee_global_limit, fixed_fee_global_limit_currency, fixed_fee_global_limit_period, reference_payment_date) VALUES (555, 555, 1400000000, "HUF", "MONTHLY", "2022-10-01");

INSERT INTO broker_product_commissions (id, product_id, market, percent_fee, minimum_fee, currency) VALUES (555, 555, "XETRA", 350000, 7000000, "EUR");
INSERT INTO broker_product_commissions (id, product_id, market, percent_fee, minimum_fee, currency) VALUES (5555, 555, "Amsterdam", 350000, 7000000, "EUR");

INSERT INTO brokerage_accounts (id, name, account_type) VALUES (555, "account", "MAIN");
INSERT INTO brokerage_accounts (id, name) VALUES (556, "account");

INSERT INTO account_product_associations (id, account_id, product_id) VALUES (555, 555, 555);
INSERT INTO account_product_associations (id, account_id, product_id) VALUES (556, 556, 555);

-- SIMPLE BROKER TWO
INSERT INTO brokers(id, name) VALUES (666, "simple two");
INSERT INTO broker_products(id, broker_id, name, fixed_fee_amt, fixed_fee_currency, fixed_fee_period, balance_fee_percent) VALUES (666, 666, "all accounts", 700000000, "HUF", "MONTHLY", 10000);
INSERT INTO broker_global_fees (id, broker_id, fixed_fee_global_limit, fixed_fee_global_limit_currency, fixed_fee_global_limit_period, reference_payment_date) VALUES (666, 666, 1400000000, "HUF", "MONTHLY", "2022-10-01");
INSERT INTO brokerage_accounts (id, name) VALUES (666, "account 1");
INSERT INTO brokerage_accounts (id, name) VALUES (667, "account 2");
INSERT INTO brokerage_accounts (id, name) VALUES (668, "account 3");
INSERT INTO account_product_associations (id, account_id, product_id) VALUES (666, 666, 666);
INSERT INTO account_product_associations (id, account_id, product_id) VALUES (667, 667, 666);
INSERT INTO account_product_associations (id, account_id, product_id) VALUES (668, 668, 666);
