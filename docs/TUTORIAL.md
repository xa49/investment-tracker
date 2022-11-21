Note: The URLs only work if every database table has a `next_val` of 1 for the auto-incremented primary key.

## Add a broker

This is the broker which manages your brokerage accounts. Brokers only need a name and we'll name this one Only Broker.

`POST`

```
http://localhost:8180/api/v1/brokers
```

```json5
{
  "name": "The Only Broker"
}
```

## Record how much the broker charges for cash withdrawals

This broker will charge 0.45% on USD withdrawals but a minimum of HUF 2,710 and a maximum of HUF 66,000. If we provided
only `minimumFee` then the fee would behave as a fixed fee. We could also provide `fromDate` and `toDate` to signal
which period this fee relates to. Null values mean no boundary.

`POST`

```
http://localhost:8180/api/v1/brokers/1/transfer-fee
```

```json5
{
  "transferredCurrency": "USD",
  "percentFee": "0.45",
  "minimumFee": "2710",
  "maximumFee": "66000",
  "feeCurrency": "HUF"
}
```

## Add a product to the broker

Brokers usually offer a single product, the trading account. We'll associate your trading accounts with this product
later on.

`POST`

```
http://localhost:8180/api/v1/brokers/1/product
```

```json5
{
  "name": "trading account"
}
```

## Record how much commission you need to pay on your trades

These commissions usually differ by stock markets. We'll pay 0.35% of the value of our trade as commission for stocks
traded on NYSE. A minimum fee of USD 7 will apply.

`POST`

```
http://localhost:8180/api/v1/brokers/1/product/1/commission
```

```json5
{
  "market": "NYSE",
  "percentFee": "0.35",
  "minimumFee": "7",
  "currency": "USD"
}
```

## It's time to add your trading accounts to the database

You will have three accounts: one MAIN account at the broker, which is compulsory plus two TBSZ accounts which have a
special tax-status - if you leave your investments in there for 5 years, you don't have to pay taxes on your gains.

`POST`

```
http://localhost:8180/api/v1/accounts
```

Your main account:

```json5
{
  "name": "my main account",
  "openedDate": "2000-01-01",
  "accountType": "MAIN"
}
```

Your first TBSZ account which will continue to be tax-exempt:

```json5
{
  "name": "TBSZ 2000 account",
  "openedDate": "2000-02-02",
  "accountType": "TBSZ"
}
```

Your second TBSZ account which will have a tax-exemption on the gains for the first 5 years and then will be broken:

```json5
{
  "name": "(eventually broken) TBSZ 2001 account",
  "openedDate": "2001-03-03",
  "accountType": "TBSZ"
}
```

## Associate these accounts with products

Normally each account is associated with one product only over its lifetime, however, the merger of two brokerage
companies is an exception to this. In this example, all your accounts will be associated with the same product.

`POST`

Main account:

```
http://localhost:8180/api/v1/accounts/1/association
```

```json5
{
  "productId": 1,
  "fromDate": "2000-01-01"
}
```

First TBSZ account:

```
http://localhost:8180/api/v1/accounts/2/association
```

```json5
{
  "productId": 1,
  "fromDate": "2000-02-02"
}
```

Second (broken) TBSZ account:

```
http://localhost:8180/api/v1/accounts/3/association
```

```json5
{
  "productId": 1,
  "fromDate": "2001-03-03"
}
```

## Create a portfolio of your 3 accounts

`POST`

```
http://localhost:8180/api/v1/analysis/portfolio
```

```json5
{
  "name": "my-portfolio",
  "accountIds": [
    1,
    2,
    3
  ]
}
```

## On a slightly unrelated note, add the taxation rules

You'll pay 15% tax on your gains (payable only when an investment is exited) and will be able to offset any realised
losses from the past two full years with each year starting on 1st January.

`POST`

```
http://localhost:8180/api/v1/tax
```

```json5
{
  "taxResidence": "HU",
  "taxationCurrency": "HUF",
  "flatCapitalGainsTaxRate": "15",
  "lossOffsetYears": 2,
  "lossOffsetCutOffMonth": 1,
  "lossOffsetCutOffDay": 1
}
```

**At this point all your accounts and related details are set up. You can check that everything is fine by
calling `GET http://localhost:8180/api/v1/analysis/actual-position` with the following body:**

```json5
{
  "portfolioName": "my-portfolio",
  "asOfDate": "2010-01-01",
  "taxResidence": "HU"
}
```

**Naturally, all balances will be empty because you
haven't recorded any transactions yet to which we'll move on now.**

# Transactions

All calls are `POST` to:

```
http://localhost:8180/api/v1/transactions
```

## Deposit USD 10,000 on your main account

```json5
{
  "date": "2000-01-02",
  "transactionType": "MONEY_IN",
  "addToAccountId": 1,
  "assetAddedId": "USD",
  "countOfAssetAdded": "10000"
}
```

## Buy 100 shares of 3M on your main account for USD 40 apiece

```json5
{
  "date": "2000-01-30",
  "transactionType": "ENTER_INVESTMENT",
  "addToAccountId": 1,
  "assetAddedId": "MMM",
  "countOfAssetAdded": "100",
  "takeFromAccountId": 1,
  "assetTakenId": "USD",
  "countOfAssetTaken": "4000"
}
```

When you make this call, you get back two transactions. One that relates to the purchase of the 100 shares and the other
relates to the commissions you pay to your broker as a result of this trade. The latter is calculated and added
automatically for you based on the details you provided. (E.g. this trade had a value of USD 4,000, and you pay the
highest of 0.35% times this value (USD 14) or the minimum fee (USD 7).)

## Transfer USD 2,500 to your TBSZ 2000 account

```json5
{
  "date": "2000-02-10",
  "transactionType": "TRANSFER_CASH",
  "addToAccountId": 2,
  "assetAddedId": "USD",
  "countOfAssetAdded": "2500",
  "takeFromAccountId": 1,
  "assetTakenId": "USD",
  "countOfAssetTaken": "2500"
}
```

## Purchase 40 Johnson & Johnson shares for USD 52 apiece

```json5
{
  "date": "2000-03-01",
  "transactionType": "ENTER_INVESTMENT",
  "addToAccountId": 2,
  "assetAddedId": "JNJ",
  "countOfAssetAdded": "40",
  "takeFromAccountId": 2,
  "assetTakenId": "USD",
  "countOfAssetTaken": "2080"
}
```

At this point you'll also have the transaction commission calculated automatically (USD 7.28).

## Transfer USD 2,500 to your (eventually) broker TBSZ 2001 account

```json5
{
  "date": "2001-02-10",
  "transactionType": "TRANSFER_CASH",
  "addToAccountId": 3,
  "assetAddedId": "USD",
  "countOfAssetAdded": "2500",
  "takeFromAccountId": 1,
  "assetTakenId": "USD",
  "countOfAssetTaken": "2500"
}
```

## Buy 30 shares of 3M for USD 55 apiece

```json5
{
  "date": "2001-03-04",
  "transactionType": "ENTER_INVESTMENT",
  "addToAccountId": 3,
  "assetAddedId": "MMM",
  "countOfAssetAdded": "30",
  "takeFromAccountId": 3,
  "assetTakenId": "USD",
  "countOfAssetTaken": "1650"
}
```

At this step, the trading commission based on the percentage-calculation would be USD 1,650 x 0.35% = USD 5.775
therefore the minimum fee of USD 7 is applied here.

## Break your TBSZ 2001 account by selling 20 shares of 3M for USD 68 apiece

```json5
{
  "date": "2008-03-04",
  "transactionType": "EXIT_INVESTMENT",
  "addToAccountId": 3,
  "assetAddedId": "USD",
  "countOfAssetAdded": "1360",
  "takeFromAccountId": 3,
  "assetTakenId": "MMM",
  "countOfAssetTaken": "20",
  "matchingStrategy": "FIFO"
}
```

You opened this TBSZ account in 2001, therefore it served the 5-year period between 2002 and 2006 untouched. You don't
have to pay taxes for this period - you only have to pay taxes compared to the 1st January 2007 share price.

## Check how much your portfolio is worth on 1st January 2010

`GET`

```
http://localhost:8180/api/v1/analysis
```

```json5
{
  "portfolioName": "my-portfolio",
  "currency": "HUF",
  "taxResidence": "HU",
  "asOfDate": "2010-01-01"
}
```
This request will take a couple of seconds because data from external source is fetched as needed. The response body will be the following:

```json5
{
    "marketValueOfSecurities": [
        {
            "amount": 11670.099940,
            "currency": "USD"
        }
    ],
    "actualCashBalance": [
        {
            "amount": 3594.72,
            "currency": "USD"
        }
    ],
    "transactionCommissions": [
        {
            "amount": 40.8453497900,
            "currency": "USD"
        }
    ],
    "transferFees": [
        {
            "amount": 12884.2780553135761500,
            "currency": "HUF"
        }
    ],
    "exchangeFees": [
        {
            "amount": 14315.864505903973500,
            "currency": "HUF"
        }
    ],
    "taxDue": {
        "amount": 132388.433894982168577500,
        "currency": "HUF"
    },
    "fullyLiquidValue": {
        "amount": 2703584.324724594981772500,
        "currency": "HUF"
    },
    "investmentReturnInPercent": 0.6845400530566524
}
```

Some of these figures can be easily verified from the previous steps:

- As of 1st January 2010, you held 110 shares in 3M and 40 in Johnson & Johnson. The closing prices of these were USD 82.67 and USD 64.41, respectively at that time, which results in a market value of USD 11,670.
- You could sell these shares as two transactions - the 3M-transaction worth USD 9,093.7 and the J&J one worth USD 2,576.4. You need to pay 0.35% of these as commissions (or a minimum of USD 7 per transaction which is exceeded in both cases here) which totals USD 40.845 of trading commission.
- You had USD 10,000 initially, then purchased shares for USD 4,000 + 2,080 + 1,650 = USD 7,730 and sold shares for USD 1,360. The total trading commissions on these were USD 35.28. You didn't have to pay taxes on your investment exit in 2008 which leads to a cash balance of USD 3,594.72.