-- brokerage_accounts definition

CREATE TABLE `brokerage_accounts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `account_type` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `closed_date` date DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `opened_date` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- brokers definition

CREATE TABLE `brokers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- currencies definition

CREATE TABLE `currencies` (
  `id` bigint(20) NOT NULL,
  `iso_code` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_6tpjdn2bms3lc6k1995a8qryg` (`iso_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- exchange_rates definition

CREATE TABLE `exchange_rates` (
  `id` bigint(20) NOT NULL,
  `date` date DEFAULT NULL,
  `destination_iso_abbreviation` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `exchange_rate` bigint(20) DEFAULT NULL,
  `source_iso_abbreviation` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- investment_asset_records definition

CREATE TABLE `investment_asset_records` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `asset_id` bigint(20) DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- portfolio_memberships definition

CREATE TABLE `portfolio_memberships` (
  `portfolio_id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- portfolios definition

CREATE TABLE `portfolios` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- securities definition

CREATE TABLE `securities` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `currency` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_hungarian_ci NOT NULL,
  `market` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `ticker` varchar(255) COLLATE utf8mb4_hungarian_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ec0wadl7yajtslcjgfhlr7j54` (`ticker`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- security_prices definition

CREATE TABLE `security_prices` (
  `id` bigint(20) NOT NULL,
  `adj_close` bigint(20) DEFAULT NULL,
  `close` bigint(20) DEFAULT NULL,
  `currency` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `date` date DEFAULT NULL,
  `high` bigint(20) DEFAULT NULL,
  `low` bigint(20) DEFAULT NULL,
  `open` bigint(20) DEFAULT NULL,
  `ticker` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `volume` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- tax_details definition

CREATE TABLE `tax_details` (
  `id` bigint(20) NOT NULL,
  `flat_capital_gains_tax_rate` bigint(20) DEFAULT NULL,
  `from_date` date DEFAULT NULL,
  `loss_offset_cut_off_day` int(11) DEFAULT NULL,
  `loss_offset_cut_off_month` int(11) DEFAULT NULL,
  `loss_offset_years` int(11) DEFAULT NULL,
  `tax_residence` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `taxation_currency` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `to_date` date DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- broker_global_fees definition

CREATE TABLE `broker_global_fees` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `balance_fee_global_limit` bigint(20) DEFAULT NULL,
  `balance_fee_global_limit_currency` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `balance_fee_global_limit_period` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `fixed_fee_global_limit` bigint(20) DEFAULT NULL,
  `fixed_fee_global_limit_currency` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `fixed_fee_global_limit_period` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `from_date` date DEFAULT NULL,
  `global_fixed_fee_amt` bigint(20) DEFAULT NULL,
  `global_fixed_fee_currency` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `global_fixed_fee_period` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `reference_payment_date` date DEFAULT NULL,
  `to_date` date DEFAULT NULL,
  `broker_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKcsmq8vg943f8lj90o8p6jooti` (`broker_id`),
  CONSTRAINT `FKcsmq8vg943f8lj90o8p6jooti` FOREIGN KEY (`broker_id`) REFERENCES `brokers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- broker_products definition

CREATE TABLE `broker_products` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `balance_fee_currency` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `balance_fee_max_amt` bigint(20) DEFAULT NULL,
  `balance_fee_percent` bigint(20) DEFAULT NULL,
  `balance_fee_period` int(11) DEFAULT NULL,
  `fixed_fee_amt` bigint(20) DEFAULT NULL,
  `fixed_fee_currency` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `fixed_fee_period` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `from_date` date DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `to_date` date DEFAULT NULL,
  `broker_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKop7yne1diy7plxn7lg0b0ts3j` (`broker_id`),
  CONSTRAINT `FKop7yne1diy7plxn7lg0b0ts3j` FOREIGN KEY (`broker_id`) REFERENCES `brokers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- broker_transfer_fees definition

CREATE TABLE `broker_transfer_fees` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `fee_currency` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `from_date` date DEFAULT NULL,
  `maximum_fee` bigint(20) DEFAULT NULL,
  `minimum_fee` bigint(20) DEFAULT NULL,
  `percent_fee` bigint(20) DEFAULT NULL,
  `to_date` date DEFAULT NULL,
  `transferred_currency` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `broker_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKi08teajvdl3as9mhpw67m28l6` (`broker_id`),
  CONSTRAINT `FKi08teajvdl3as9mhpw67m28l6` FOREIGN KEY (`broker_id`) REFERENCES `brokers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- transactions definition

CREATE TABLE `transactions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `count_of_asset_added` bigint(20) DEFAULT NULL,
  `count_of_asset_taken` bigint(20) DEFAULT NULL,
  `date` date DEFAULT NULL,
  `fee_amount` bigint(20) DEFAULT NULL,
  `fee_currency` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `fee_type` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `matching_strategy` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `transaction_type` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `add_account_id` bigint(20) DEFAULT NULL,
  `asset_added_record_id` bigint(20) DEFAULT NULL,
  `asset_taken_record_id` bigint(20) DEFAULT NULL,
  `take_account_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhmg5iyi54kjkpres5aylce9g3` (`add_account_id`),
  KEY `FKh8j5fl0e24r3l2dgcfxfwrta` (`asset_added_record_id`),
  KEY `FKmuq0vpyywm83t4k44ujlbllb2` (`asset_taken_record_id`),
  KEY `FKin5np832sahgskt5xwv6pbsqd` (`take_account_id`),
  CONSTRAINT `FKh8j5fl0e24r3l2dgcfxfwrta` FOREIGN KEY (`asset_added_record_id`) REFERENCES `investment_asset_records` (`id`),
  CONSTRAINT `FKhmg5iyi54kjkpres5aylce9g3` FOREIGN KEY (`add_account_id`) REFERENCES `brokerage_accounts` (`id`),
  CONSTRAINT `FKin5np832sahgskt5xwv6pbsqd` FOREIGN KEY (`take_account_id`) REFERENCES `brokerage_accounts` (`id`),
  CONSTRAINT `FKmuq0vpyywm83t4k44ujlbllb2` FOREIGN KEY (`asset_taken_record_id`) REFERENCES `investment_asset_records` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- account_product_associations definition

CREATE TABLE `account_product_associations` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `from_date` date DEFAULT NULL,
  `to_date` date DEFAULT NULL,
  `account_id` bigint(20) DEFAULT NULL,
  `product_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkutg2votn87yrba6cvkwlr1jr` (`account_id`),
  KEY `FK5mt0fsjybisq71yrdykclb60x` (`product_id`),
  CONSTRAINT `FK5mt0fsjybisq71yrdykclb60x` FOREIGN KEY (`product_id`) REFERENCES `broker_products` (`id`),
  CONSTRAINT `FKkutg2votn87yrba6cvkwlr1jr` FOREIGN KEY (`account_id`) REFERENCES `brokerage_accounts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;


-- broker_product_commissions definition

CREATE TABLE `broker_product_commissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `currency` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `from_date` date DEFAULT NULL,
  `market` varchar(255) COLLATE utf8mb4_hungarian_ci DEFAULT NULL,
  `maximum_fee` bigint(20) DEFAULT NULL,
  `minimum_fee` bigint(20) DEFAULT NULL,
  `percent_fee` bigint(20) DEFAULT NULL,
  `to_date` date DEFAULT NULL,
  `product_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKxtvn00593l06gxil3cja46qp` (`product_id`),
  CONSTRAINT `FKxtvn00593l06gxil3cja46qp` FOREIGN KEY (`product_id`) REFERENCES `broker_products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;

-- hibernate_sequence definition

CREATE TABLE `hibernate_sequence` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;

-- rate_seq definition

CREATE TABLE `rate_seq` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_hungarian_ci;

INSERT INTO `hibernate_sequence` VALUES (1);
INSERT INTO `rate_seq` VALUES (1);
