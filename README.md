## Purpose of this application

Let's say you have shares worth $1,000,000 in your brokerage account. Can you claim you are a millionaire?

Well, sure,
just look at the balance, that's what everybody does. But the practical answer is, unfortunately, no, for a number of
reasons:

* **Transaction commissions:** Very few businesses accept shares as payment, so first you need to sell shares for cash
  and
  give (most commonly) a percentage fee to your broker for arranging this
* **Transfer fees:** After the previous step, you have a cash balance in your *brokerage* account, but you still cannot
  spend that because brokerage accounts don't function as payment accounts. Therefore, you need to transfer the cash to
  your *bank* account triggering further charges by your broker
* **Currency conversion fees:** You might be interested in being a euro-millionaire as opposed to being
  dollar-millionaire, but to have euros you have to exchange your dollars. Your bank or another party, like Wise or
  Revolut, commonly charge you for this
* **Capital gains taxes:** If you made a profit on your investments, you normally have to pay some tax as well, although
  government-sponsored savings accounts might offer you a tax break under certain circumstances

**✅ Investment tracker tells you EXACTLY how much money you can spend taking all these into account.**

## Features

* Calculation of the true, realisable portfolio value as described above and the corresponding investment return (in
  percent) as of any past date
* Providing a snapshot of all the assets you have (and had in the past) in your portfolio
* Automatic calculation of certain fees that can be used to reduce taxes
* Integrated APIs for querying up-to-date exchange rates and security prices
* Support for tax-exempt [TBSZ accounts](https://www.allamkincstar.gov.hu/en/faq/content/6186/) and automatic tax
  calculation based on complex tax rules
* Pre-loadable dataset for testing the application

## Setting up the API

It is recommended to run the API in Docker. You need to have port 8180 available for the API and port 13306 for the
exposed MariaDB.

```shell
git clone https://github.com/xa49/investment-tracker
```

```shell
cd investment-tracker
```

The following can optionally be called with `-DskipTests`:

```shell
./mvnw clean package
```

```shell
docker build -t investment-tracker .
```

```shell
docker compose up
```

## Usage

The portfolio value and investment returns depend on a large number of parameters. Among others:

* All your transactions (depositing and withdrawing money, buying and selling shares, transferring among accounts) need
  to be recorded because your investment return (in %) depends on the timing of your transactions.
* The fees charged by your broker are required to calculate by what amount you can decrease your tax liability.
* The type of each of your brokerage accounts (e.g. regular account or TBSZ) is also required to determine which tax
  calculation rule to apply.

Before any analysis of your portfolio, these details need to be added which is a fair amount of work. If you want to
just test the application with sample data, [see how you can do it](#pre-configured-data-for-testing). Otherwise,
the [Setting up your transaction history](#setting-up-your-transaction-history) section guides you through this step.

When you are done configuring, you can start the analysis of your portfolio.

(Note: No market data comes preloaded with the application. Normally the reason for 1+ sec response time is the fetching
& persisting of additional market data from Yahoo Finance or the MNB SOAP service.)

### See details about the true, realisable value of your portfolio by calling `/api/v1/analysis`

In the body of the request, you need to specify how you would like to have your query evaluated. If you want to see the
value of your portfolio in USD and have it calculated as of 15th November 2022, then use:

```json5
{
  "portfolioName": "my-portfolio",
  "currency": "USD",
  "taxResidence": "HU",
  "asOfDate": "2022-11-15"
}
```

You will then receive the detailed analysis of your portfolio in JSON:

```json5
{
  // The market value of the securities you currently hold
  "marketValueOfSecurities": [
    {
      "amount": 21329.800070,
      "currency": "USD"
    }
  ],
  // The cash balance currently held at the brokerage accounts
  "actualCashBalance": [
    {
      "amount": 3594.72,
      "currency": "USD"
    }
  ],
  // The transaction commissions that would have to be paid for an immediate fire sale of shares
  "transactionCommissions": [
    {
      "amount": 74.6543002450,
      "currency": "USD"
    }
  ],
  // The transfer fees that would have to be paid to transfer everything from the brokerage account to a bank account
  "transferFees": [
    {
      "amount": 43624.9333534356927000,
      "currency": "HUF"
    }
  ],
  // Because all sales proceeds are in USD, nothing has to be converted to another currency
  "exchangeFees": [],
  // Tax that would have to be paid on realising the investment gains
  "taxDue": {
    "amount": 847977.314811507736095000,
    "currency": "HUF"
  },
  // The true value of your portfolio you could actually spend
  "fullyLiquidValue": {
    "amount": 22564.409376427450974116843802422898685000,
    "currency": "USD"
  },
  // Your money increased by an average 3.6% per year in USD terms
  "investmentReturnInPercent": 3.619984547541439
}
```

### Get a snapshot of your holdings in your portfolio by calling `/api/v1/analysis/portfolio/actual-position`

In the body of your request, specify the details of your request:

```json
{
  "portfolioName": "my-portfolio",
  "asOfDate": "2022-01-01",
  "taxResidence": "HU"
}
```

You will then see the holdings in your portfolio on the 1st January 2022:

```json5
{
  "cashBalance": {
    "personal main account": {
      "USD": 986,
      "EUR": 1210
    }
  },
  "securityBalance": {
    "tbsz 2000 account": {
      "3M Company": 40
    }
  },
  // These are past fees paid on transactions and can be used to offset taxable gains
  "feeWriteOffAvailable": {
    "amount": 8879.885,
    "currency": "HUF"
  },
  // These are losses you realised in the past and can be used to offset taxable gains for a while
  "lossOffsetsRecorded": [
    {
      "amount": 35981.13782560,
      "currency": "HUF",
      "date": "2008-03-04"
    }
  ]
}
```

## Pre-configured data for testing

To load the preconfigured data, make a GET request to `/api/v1/test-api/preload`. This will add three brokerage
accounts (one
main, two TBSZ), all at the same broker (check these at the `GET /api/v1/accounts` endpoint).

The transfer fees are set for USD (0.45%, min. HUF 2,710, max. HUF 66,000) and commissions are set for NYSE (0.35%, min.
USD 7). The following transactions are also added to the three accounts:

Main account:

1. Deposit USD 10,000 in January 2000
2. Buy 100 shares of 3M for USD 40 each in January 2000
3. Transfer USD 2,500 to TBSZ 2000 account in February 2000
4. Transfer USD 2,500 to TBSZ 2001 account in March 2001

TBSZ 2000 account:

1. Receive USD 2,500 from main account in February 2000
2. Buy 40 shares of 3M for USD 43 each in March 2000

TBSZ 2001 account:

1. Receive USD 2,500 from main account in March 2001
2. Buy 40 shares of 3M for USD 55 each in March 2001
3. Sell 20 shares of 3M for USD 68 each in March 2008

With the last transaction, the TBSZ 2001 account is not a valid TBSZ account anymore, however, it still completed its
first 5-year cycle, therefore it is taxable only from 1st January 2007. This means that the sale at USD 68 created a
loss, because on 1st January 2007, 3M had a share price of around USD 78.

To reset every broker entity, call `/api/v1/test-api/reset`

## Setting up your transaction history

The following entities need to be added:

1. **Tax details** - the taxes that you would have to pay and the rules governing how much loss you can offset against
   gains

New tax detail endpoint:  `POST /api/v1/tax`

Request body:

```json5
{
  "taxResidence": "HU",
  "taxationCurrency": "HUF",
  "flatCapitalGainsTaxRate": 15,
  "fromDate": "2000-01-01",
  // could omit dates from the request if there were no changes in details
  "toDate": null,
  "lossOffsetYears": 2,
  // if a loss is realised in the past 2 years, that can be offset against taxable gains
  "lossOffsetCutOffMonth": 1,
  // each year starts on 1st January for cut-off year calculation
  "lossOffsetCutOffDay": 1
}
```

2. **Broker**

New broker endpoint: `POST /api/v1/brokers`

Request body:

```json5
{
  "name": "Premium Broker"
}
```

3. **Transfer fees** - charges made by your broker for withdrawing cash

New transfer fee endpoint: `POST /api/v1/brokers/{brokerId}/transfer-fee`

Request body:

```json5
{
  "transferredCurrency": "USD",
  // this fee is charged when USD is withdrawn
  "fromDate": "2020-01-01",
  // could omit dates if this is the only transfer fee at the broker for USD
  "toDate": null,
  "percentFee": 0.45,
  "minimumFee": 2710,
  "maximumFee": 66000,
  "feeCurrency": "HUF"
  // minimumFee and maximumFee are represented in HUF
}
```

4. **Broker products** - a product is a trading account with a collection of trading commissions linked to them

Normally, you only need to add one product per broker. Only add a new broker product is not all your account attract the
same trading commissions.

New broker product endpoint: `POST /api/v1/brokers/{brokerId}/product`

Request body:

```json5
{
  "name": "trading account",
  "fromDate": "2000-01-01",
  // could omit dates
  "toDate": null
}
```

5. **Trading commissions** for broker product - the fees you pay to buy & sell shares

Net trading commission endpoint: `POST /api/v1/brokers{brokerId}/product/{productId}/commission`

Request body:

```json5
{
  "market": "NYSE",
  "percentFee": 0.35,
  "minimumFee": 7,
  "maximumFee": null,
  // could omit if not applicable
  "currency": "USD",
  // fee is charged in this currency
  "fromDate": "2020-01-01",
  "toDate": null
}
```

6. **Brokerage accounts** - each brokerage account you have at your brokers

New brokerage account endpoint: `POST /api/v1/accounts`

Request body:

```json5
{
  "name": "tbsz 2000 account",
  "openedDate": "2000-02-02",
  // account open dates should be added
  "closedDate": null,
  // could omit if account is still open
  "accountType": "TBSZ"
  // at each broker, you must have one MAIN account at any time, plus you can have REGULAR accounts as well
}
```

7. **Account-product associations** - which products your accounts used

New account-product association endpoint: `POST /api/v1/accounts/{accountId}/association`

Request body:

```json5
{
  "productId": 10,
  "fromDate": "2020-01-01",
  // dates should be added
  "toDate": "2021-12-31"
}
```

8. **Transactions** - every deposit, withdrawal or investment decision you made

New transaction endpoint: `POST /api/v1/transactions`

Request body:

```json5
{
  "date": "2020-01-01",
  "transactionType": "EXIT_INVESTMENT",
  // allowed values: MONEY_IN, MONEY_OUT, ENTER_INVESTMENT, EXIT_INVESTMENT, PAY_FEE, TRANSFER_SECURITY, TRANSFER_CASH
  "takeFromAccountId": 12,
  "assetTakenId": "MMM",
  // the ticker of the security you sold
  "countOfAssetTaken": 10,
  "addToAccountId": 12,
  "assetAddedId": "USD",
  // the currency code of your sales proceeds
  "countOfAssetAdded": 1800,
  // "feeType": "FIXED" - used when transaction type is PAY_FEE. Values can be: FIXED, BALANCE_BASED (transfer and tranasction fees are calculated automatically)
  "matchingStrategy": "FIFO"
  // used when transaction type is EXIT_INVESTMENT or TRANSFER_SECURITY
}
```

**Disclaimer**: THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.