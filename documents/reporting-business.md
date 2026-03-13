# AI Prompt: Liquor Shop Business Intelligence & Reporting System

## Role

You are an **experienced retail business consultant, POS system architect, and data analyst** specializing in **liquor retail shop management systems**.

Your task is to design a **complete reporting and analytics system** for a **Liquor Shop Inventory & Billing Application** that helps the shop owner make **better business decisions, increase profit, optimize inventory, and plan future purchases.**

Think deeply and analyze from the perspective of a **liquor shop owner managing daily operations and long-term growth.**

---

# Business Context

The system is being built for a **liquor retail shop** where:

- The shop sells liquor to:
    - **Walk-in customers**
    - **Hotels / Restaurants / Bars**
- Hotels may purchase on **credit**
- Products are purchased using **VAT bills from suppliers**
- The **same product can have multiple purchase prices** over time
- Profit must be calculated using **Weighted Average Cost**
- Inventory must be tracked **per bottle/unit**
- The owner wants **complete visibility of stock, profit, and sales trends**

---

# Objectives

Design a **comprehensive reporting system** that helps answer these business questions:

1. How much **profit** is the shop making?
2. Which **products generate the most profit**?
3. Which **products sell the most and which are slow moving**?
4. Where is **money stuck in inventory**?
5. Which **customers generate the most revenue**?
6. How much **credit is pending from hotels or bars**?
7. What should the shop **purchase more or less in the future**?
8. How can the owner detect **profit leakage or overstocking**?

---

# 1. Core Business Reports

List and design essential reports required for a liquor shop.

For each report explain:

- Purpose
- Key metrics
- Example output

## Required Core Reports

### Daily Sales Report
Purpose: Understand daily shop performance.

Metrics:
- Total sales
- Total profit
- Number of bills
- Walk-in vs hotel sales
- Payment methods

Example:

| Metric | Value |
|------|------|
| Total Sales | NPR 120,000 |
| Total Profit | NPR 22,000 |
| Walk-in Sales | NPR 70,000 |
| Hotel Sales | NPR 50,000 |
| Bills Generated | 95 |

---

### Profit Analysis Report

Purpose: Identify which products generate the most profit.

Example:

| Product | Sold | Profit |
|-------|------|------|
| Jack Daniels | 15 | NPR 9,000 |
| Tuborg Beer | 90 | NPR 4,500 |

---

### Customer Type Profit Report

Purpose: Compare profit generated from different customer types.

| Customer Type | Sales | Profit |
|---------------|------|------|
| Walk-in | 1,200,000 | 210,000 |
| Hotels | 900,000 | 120,000 |

---

### Best Selling Product Report

Purpose: Identify top selling products.

| Product | Quantity Sold |
|-------|------|
| Tuborg Beer | 1,200 |
| Old Durbar | 650 |
| Khukuri Rum | 600 |

---

### Slow Moving Product Report

Purpose: Detect products that are not selling.

| Product | Last Sold |
|------|------|
| Chivas Regal | 30 days ago |
| Imported Wine | 45 days ago |

---

### Stock Value Report

Purpose: Know total inventory investment.

| Category | Stock Value |
|------|------|
| Whisky | NPR 800,000 |
| Beer | NPR 200,000 |
| Vodka | NPR 150,000 |

---

### Purchase vs Sales Report

Purpose: Compare purchasing behavior against sales.

| Month | Purchase | Sales |
|------|------|------|
| Jan | 1.2M | 1.0M |
| Feb | 800K | 1.1M |

---

### Supplier Purchase Report

Purpose: Track supplier dependency.

| Supplier | Total Purchase |
|------|------|
| Himalayan Distributor | 600,000 |
| Nepal Liquor Supplier | 450,000 |

---

### Credit Customer Report

Purpose: Track pending payments.

| Customer | Credit Amount |
|------|------|
| Hotel Everest | 120,000 |
| Bar Kathmandu | 80,000 |

---

### Stock Movement Report

Purpose: Track all inventory changes.

| Date | Product | Type | Qty |
|------|------|------|------|
| Mar 10 | Tuborg | Purchase | +100 |
| Mar 11 | Tuborg | Sale | -30 |

---

### Price Change History

Purpose: Track selling price updates.

| Product | Old Price | New Price | Date |
|------|------|------|------|
| Jack Daniels | 9,500 | 9,900 | Feb 10 |

---

### Category Performance Report

| Category | Sales |
|------|------|
| Whisky | 40% |
| Beer | 35% |
| Rum | 15% |
| Vodka | 10% |

---

### Monthly Growth Report

| Month | Sales | Growth |
|------|------|------|
| Jan | 900K | - |
| Feb | 1.1M | +22% |
| Mar | 1.3M | +18% |

---

### Daily Shop Closing Report

| Item | Amount |
|------|------|
| Opening Cash | 10,000 |
| Sales | 95,000 |
| Expenses | 5,000 |
| Closing Cash | 100,000 |

---

# 2. Advanced Analytics Reports

These reports provide deeper strategic insights.

### Product Margin Analysis

| Product | Cost | Sell | Margin |
|------|------|------|------|
| Old Durbar | 1,200 | 1,450 | 250 |

---

### Inventory Turnover Report

Purpose: Measure how fast products are sold.

High turnover products should be stocked more frequently.

---

### Seasonal Sales Analysis

Liquor sales often increase during:

- Festivals
- Holidays
- Weekends

Example:

| Month | Sales |
|------|------|
| Dashain | 2.5M |
| Normal Month | 1.1M |

---

### Demand Forecasting

Predict future product demand using historical sales.

Example Insight:

> Tuborg demand increases every weekend.

---

### Profit Leakage Detection

Detect issues such as:

- Excessive discounts
- Price overrides
- Wastage or breakage

---

### Reorder Prediction

System suggests when to reorder stock.

Example:

> "Tuborg Beer stock will finish in 5 days."

---

### Customer Buying Pattern Analysis

Example:

> Hotel Everest buys beer every Tuesday.

---

# 3. Owner Dashboard Design

Design a real-time dashboard showing key metrics.

Example KPIs:

- Today's Sales
- Today's Profit
- Best Selling Product Today
- Low Stock Alerts
- Pending Credit
- Total Stock Value
- Category Performance

Example Dashboard:

| Metric | Value |
|------|------|
| Today's Sales | NPR 85,000 |
| Today's Profit | NPR 15,000 |
| Low Stock Products | 6 |
| Pending Credit | NPR 200,000 |

---

# 4. Inventory Intelligence

Smart inventory insights:

- Dead stock detection
- Overstock detection
- Fast moving product alerts
- Low stock alerts
- Supplier dependency analysis

Example Insight:

> "Chivas Regal has not sold in 40 days."

---

# 5. Data Model Requirements

To generate these reports, the system should store:

### Product
- id
- name
- brand
- category
- barcode

### Purchase
- id
- supplier
- invoice number
- purchase date

### Purchase Item
- product
- quantity
- purchase price
- VAT

### Sale
- id
- customer
- sale date
- payment type

### Sale Item
- product
- quantity
- selling price

### Customer
- id
- name
- type (walk-in / hotel)

### Supplier
- id
- name
- contact

### Stock Ledger
Tracks all stock movement.

### Price History
Stores historical selling prices.

---

# 6. Profit Calculation Strategy

Profit must be calculated using **Weighted Average Cost**.

Why?

Liquor products are purchased at different prices over time.

Example:

Purchase 1  
100 bottles @ 500

Purchase 2  
100 bottles @ 600

Weighted Average Cost:



Selling price = 700

Profit per bottle = 150

---

# 7. Smart Insights for the Owner

The system should generate automatic insights.

Examples:

- "Tuborg Beer sales increased 20% this month."
- "Jack Daniels has the highest margin in Whisky category."
- "Chivas Regal has not sold for 45 days."
- "Vodka category inventory value is too high."

---

# 8. Future AI Features

Advanced features for the system:

### Demand Prediction
Predict future product demand.

### Smart Reorder Suggestions
Automatically recommend purchase quantities.

### Dynamic Pricing
Suggest price changes based on demand.

### Customer Purchase Analysis
Identify customer buying behavior.

---

# Goal

The system should not just track sales but act as a **Business Intelligence Platform for Liquor Retail Management** helping the shop owner:

- Increase profits
- Reduce dead stock
- Optimize inventory
- Improve supplier strategy
- Make smarter purchasing decisions