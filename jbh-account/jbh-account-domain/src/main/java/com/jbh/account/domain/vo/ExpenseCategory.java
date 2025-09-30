package com.jbh.account.domain.vo;

/**
 * 1. Housing
 *
 * <p>Rent / Mortgage
 *
 * <p>Property Taxes
 *
 * <p>Home Insurance
 *
 * <p>Utilities (Water, Gas, Electricity)
 *
 * <p>Internet & TV
 *
 * <p>Maintenance / Repairs
 *
 * <p>2. Transportation
 *
 * <p>Car Payment / Lease
 *
 * <p>Fuel / Charging
 *
 * <p>Public Transport
 *
 * <p>Ride-Sharing (Uber, Lyft, etc.)
 *
 * <p>Parking / Tolls
 *
 * <p>Maintenance (Oil change, Tires, Repairs)
 *
 * <p>Vehicle Insurance
 *
 * <p>3. Food & Groceries
 *
 * <p>Groceries
 *
 * <p>Dining Out / Takeout
 *
 * <p>Coffee Shops
 *
 * <p>Meal Delivery Services (Uber Eats, DoorDash)
 *
 * <p>4. Health & Wellness
 *
 * <p>Health Insurance
 *
 * <p>Doctor / Dentist / Specialist Visits
 *
 * <p>Pharmacy / Medications
 *
 * <p>Gym / Fitness Subscriptions
 *
 * <p>Therapy / Mental Health
 *
 * <p>5. Debt Payments
 *
 * <p>Credit Card Payments
 *
 * <p>Student Loan Payments
 *
 * <p>Personal Loan Payments
 *
 * <p>6. Savings & Investments
 *
 * <p>Emergency Fund
 *
 * <p>Retirement Contributions
 *
 * <p>Brokerage / Investment Accounts
 *
 * <p>Big-Purchase Savings (Vacation, Car, House Down Payment)
 *
 * <p>7. Insurance (Non-Health)
 *
 * <p>Life Insurance
 *
 * <p>Disability Insurance
 *
 * <p>Auto Insurance (if not in Transportation)
 *
 * <p>Home / Renters Insurance (if not in Housing)
 *
 * <p>8. Personal Spending
 *
 * <p>Clothing & Shoes
 *
 * <p>Beauty & Grooming (Haircuts, Cosmetics)
 *
 * <p>Subscriptions (Netflix, Spotify, etc.)
 *
 * <p>Hobbies / Sports
 *
 * <p>Gadgets & Electronics
 *
 * <p>9. Entertainment & Leisure
 *
 * <p>Events (Concerts, Movies, Sports)
 *
 * <p>Books / Games / Apps
 *
 * <p>Travel & Vacations
 *
 * <p>10. Education & Self-Improvement
 *
 * <p>Tuition / Courses
 *
 * <p>Books & Learning Materials
 *
 * <p>Certifications
 *
 * <p>11. Family & Kids
 *
 * <p>Childcare / Babysitting
 *
 * <p>School Supplies & Fees
 *
 * <p>Activities / Lessons
 *
 * <p>12. Gifts & Donations
 *
 * <p>Gifts (Birthdays, Holidays)
 *
 * <p>Charitable Donations
 *
 * <p>13. Miscellaneous / Unexpected
 *
 * <p>Pet Expenses
 *
 * <p>Bank Fees
 *
 * <p>Anything uncategorized
 *
 * <p>Essential Categories:
 *
 * <p>Housing - Rent, mortgage, utilities Food - Groceries, dining out Transport - Gas, public
 * transit, car payments Health - Medical, insurance, pharmacy Utilities - Electric, water,
 * internet, phone
 *
 * <p>Lifestyle Categories:
 *
 * <p>Shopping - Clothes, personal items Entertainment - Movies, hobbies, subscriptions Personal -
 * Haircuts, cosmetics, cigarettes Education - Courses, books, training Travel - Trips, hotels,
 * flights
 *
 * <p>Financial Categories:
 *
 * <p>Savings - Emergency fund, goals Investments - Stocks, bonds, crypto Debt - Credit cards, loans
 * Insurance - Life, auto, home Taxes - Income tax, property tax
 *
 * <p>Other:
 *
 * <p>Gifts - Presents, donations Pets - Food, vet, supplies Misc - Uncategorized items
 */
public enum ExpenseCategory implements CategoryType {
  RETEFUENTE(0),
  SOCIAL_SECURITY(1),
  PUBLIC_SERVICES(2),
  PERSONAL(3);

  private final int categoryId;

  ExpenseCategory(final int value) {
    this.categoryId = value;
  }

  @Override
  public CategorySource getSource() {
    return CategorySource.EXPENSE;
  }

  @Override
  public String toString() {
    return "ExpenseCategory{" + "categoryId=" + categoryId + '}';
  }
}
